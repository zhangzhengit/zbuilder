package com.vo.service;

import java.io.File;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;

import com.mysql.cj.exceptions.ClosedOnExpiredPasswordException;
import com.vo.anno.ZAutowired;
import com.vo.anno.ZComponent;
import com.vo.conf.Conf;
import com.vo.conn.ZDatasourceProperties.P;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.dto.DeployDTO;
import com.vo.entity.BuildHistoryEntity;
import com.vo.entity.JobEntity;
import com.vo.entity.NodeEntity;
import com.vo.repository.BuildHistoryRepository;
import com.vo.repository.JobRepository;
import com.votool.common.CR;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月26日
 *
 */
@ZComponent
public class BuildHistoryService {

	private static final ZLog2 LOG = ZLog2.getInstance();

	@ZAutowired
	private JobRepository jobRepository;
	@ZAutowired
	private JobService jobService;
	@ZAutowired
	private BuildHistoryRepository buildHistoryRepository;

	public  CR<Object> deploy(final DeployDTO deployDTO) {
		LOG.info("开始部署,deployDTO={}", deployDTO);
		final Conf conf = ZContext.getBean(Conf.class);

		final BuildHistoryEntity entity = this.buildHistoryRepository.findById(deployDTO.getHId());
		if (entity == null) {
			return CR.error("构建信息不存在");
		}
		final Integer v1 = entity.getVersion();
		final String v2 = deployDTO.getVersion();
		if (!Objects.equals(String.valueOf(v1), v2)) {
			return CR.error("参数错误，id和version不匹配");
		}

		final JobEntity jobEntity = this.jobRepository.findById(entity.getJobId());

		// FIXME 2023年9月27日 下午6:59:32 zhanghen: 开始执行java -jar
		final List<NodeEntity> nodeList = this.jobService.findNodeInfoById(entity.getJobId());
		for (final NodeEntity nodeEntity : nodeList) {
			LOG.info("开始部署机器,nodeIp={}", nodeEntity.getIp());
//			ssh -fv root@192.168.1.5 "nohup java -jar /root//gangdan/sb_jenkins_test_20230923/target/sb_jenkins_test_20230923-1.0-SNAPSHOT.jar >/dev/null 2>&1 &"

			final String deploy = "ssh -fv root@" + nodeEntity.getIp() + " nohup java -jar "

					 + conf.getWorkspace() + File.separator
					;
		}

//		entity.getJobId();2l

		return CR.ok();
	}

	public BuildHistoryEntity createOne(final BuildHistoryEntity buildHistoryEntity) {
		if (StrUtil.isNotEmpty(buildHistoryEntity.getOutput())) {
			buildHistoryEntity.setOutput(buildHistoryEntity.getOutput().replace("'", "\\'"));
		}
		final BuildHistoryEntity save = this.buildHistoryRepository.save(buildHistoryEntity);
		return save;
	}

	public Integer findMaxVersion(final Integer jobId) {
		final List<BuildHistoryEntity> list = this.buildHistoryRepository.findByJobId(jobId);
		if (CollUtil.isEmpty(list)) {
			return 0;
		}
		// FIXME 2023年9月27日 下午3:57:39 zhanghen: 先该ZR 支持 类似findMaxXXByXX

		final Stream<Date> sorted = list.stream().map(e -> e.getCreateTime()).sorted(Comparator.reverseOrder());
		final BuildHistoryEntity max = list.stream().max(Comparator.comparing(BuildHistoryEntity::getCreateTime)).get();
		return max.getVersion();
	}

	public void appendOutputAndInsertResult(final BuildHistoryEntity buildHistoryEntity) {

		synchronized (buildHistoryEntity.getUuid()) {

			if (StrUtil.isNotEmpty(buildHistoryEntity.getOutput())) {
				buildHistoryEntity.setOutput(buildHistoryEntity.getOutput().replace("'", "\\'"));
			}

			final List<BuildHistoryEntity> list = this.buildHistoryRepository.findByUuid(buildHistoryEntity.getUuid());
			if (CollUtil.isEmpty(list)) {
				this.buildHistoryRepository.save(buildHistoryEntity);
			} else {
				final BuildHistoryEntity e = list.get(0);
				if (e.getOutput() == null) {
					e.setOutput(buildHistoryEntity.getOutput());
				} else {
					e.setOutput(e.getOutput() + buildHistoryEntity.getOutput());
				}
				e.setResult(buildHistoryEntity.getResult());
				e.setCreateTime(e.getCreateTime());
				this.buildHistoryRepository.update(e);
			}
		}
	}

}
