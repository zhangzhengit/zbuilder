package com.vo.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.vo.anno.ZAutowired;
import com.vo.anno.ZComponent;
import com.vo.anno.ZRequestBody;
import com.vo.api.BuildService;
import com.vo.api.NewNodeDTO;
import com.vo.api.StopNodeDTO;
import com.vo.core.ZLog2;
import com.vo.dto.KillJobDTO;
import com.vo.dto.StartJobDTO;
import com.vo.entity.JobEntity;
import com.vo.entity.NodeEntity;
import com.vo.enums.NodeStatusEnum;
import com.vo.service.JobService;
import com.vo.service.SVNService;
import com.votool.common.CR;
import com.votool.ze.AbstractZETask;
import com.votool.ze.ZE;
import com.votool.ze.ZES;
import com.votool.ze.ZETaskResult;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import groovy.util.IFileNameFinder;
import groovyjarjarantlr.FileLineFormatter;
import io.lettuce.core.output.ListSubscriber;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月29日
 *
 */
@ZComponent
public class NodeService {

	public static final String YUNXINGZHONG = "✔运行中";

	public static final String WEI_YUNXING = "×未运行";

	private static final ZLog2 LOG = ZLog2.getInstance();
	private static final ZE ZE = ZES.newZE();

	public static final String IP_REGEX = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
	private static final Pattern IP_PATTERN = Pattern.compile(IP_REGEX);

	@ZAutowired
	private JobService jobService;
	@ZAutowired
	private JobRepository jobRepository;
	@ZAutowired
	private NodeRepository nodeRepository;

	public CR<Object> killJobAllNode(final KillJobDTO killJobDTO) {
		LOG.info("开始停止所有节点上的应用,killJobDTO={}", killJobDTO);

		final JobEntity jobEntity = this.jobRepository.findById(killJobDTO.getJobId());
		LOG.info("开始停止所有节点上的应用[{}]", jobEntity.getProjectName());

		final List<Integer> nodeIdList = this.jobService.parseNodeIdArray(jobEntity);
		final List<NodeEntity> nodeList = this.nodeRepository.findByIdIn(nodeIdList);

		final StringBuilder builder = new StringBuilder();
		for (final NodeEntity nodeEntity : nodeList) {
			final String killJobOutput= NodeService.this.killJob0(nodeEntity, jobEntity);
			builder.append(killJobOutput).append("<br/");
		}

		return CR.ok(builder.toString());
	}

	public CR<Object> killJob(final KillJobDTO killJobDTO) {
		LOG.info("开始停止节点上的应用,killJobDTO={}", killJobDTO);

		final NodeEntity nodeEntity = this.nodeRepository.findById(killJobDTO.getNodeId());
		final JobEntity jobEntity = this.jobRepository.findById(killJobDTO.getJobId());
		LOG.info("开始停止节点[{}]上的应用[{}]", nodeEntity.getNickName(),jobEntity.getProjectName());

//		ssh -v root@192.168.1.5 'kill $(pgrep -f sb_jenkins_test_20230923-1.0-SNAPSHOT.jar)'

		final String killOutput = this.killJob0(nodeEntity, jobEntity);

		return CR.ok(killOutput);

	}

	private String killJob0(final NodeEntity nodeEntity, final JobEntity jobEntity) {
		// 改用pkill -f 杀死全部的进程
		final String kill = "ssh -v root@"  + nodeEntity.getIp() + " " + "\'pkill -f " + jobEntity.getProjectName() + "\'";
//		final String kill = "ssh -v root@"  + nodeEntity.getIp() + " " + "\'kill $(pgrep -f " + jobEntity.getProjectName() + ")\'";
		final String killOutput = SVNService.executeLinux("kill-pid", kill);
		LOG.info("killOutput={}", killOutput);
		return killOutput;
	}

	public CR<String> startJob(final StartJobDTO startJobDTO) {
		LOG.info("开始启动节点上的应用,startJobDTO={}", startJobDTO);

		final NodeEntity nodeEntity = this.nodeRepository.findById(startJobDTO.getNodeId());
		final JobEntity jobEntity = this.jobRepository.findById(startJobDTO.getJobId());
		LOG.info("开始启动节点[{}]上的应用[{}]", nodeEntity.getNickName(),jobEntity.getProjectName());

		final String deployOutput = this.startJob0(nodeEntity, jobEntity);

		return CR.ok(deployOutput);
	}

	private String startJob0(final NodeEntity nodeEntity, final JobEntity jobEntity) {
		final String deployCD1 = "ssh -fv root@" + nodeEntity.getIp()
				+ " " + "\'nohup java -jar "
				+ jobEntity.getTargetDirectory()
				 + File.separator + "jar" +  File.separator
				 + jobEntity.getProjectName() + File.separator + BuildService.findJarFile(jobEntity).getName()
				+ " >> " + jobEntity.getLogFilePath() + " &'"
				;

		final String deploy = "ssh -fv root@" + nodeEntity.getIp()
				+ " " + "\'cd "
				+ jobEntity.getTargetDirectory()
				+ File.separator + "jar"
				+ File.separator
				+ jobEntity.getProjectName()
				+ " && nohup java -jar "
				+ BuildService.findJarFile(jobEntity).getName()
				+ " >> " + jobEntity.getLogFilePath() + " &'"
				;

		final String deployOutput = SVNService.executeLinux("start-job", deploy);
		return deployOutput;
	}

	public CR<String> startJobAllNode(final StartJobDTO startJobDTO) {
		LOG.info("开始启动所有节点上的应用,startJobDTO={}", startJobDTO);
		final JobEntity jobEntity = this.jobRepository.findById(startJobDTO.getJobId());
		LOG.info("开始启动所有节点上的应用[{}]", jobEntity.getProjectName());

		final List<Integer> nodeIdList = this.jobService.parseNodeIdArray(jobEntity);
		final List<NodeEntity> nodeList = this.nodeRepository.findByIdIn(nodeIdList);

		final ArrayList<AbstractZETask<String>> taskList = Lists.newArrayList();
		for (final NodeEntity nodeEntity : nodeList) {
			final AbstractZETask<String> task = new AbstractZETask<String>() {

				@Override
				public String call() {
					final String deployOutput = NodeService.this.startJob0(nodeEntity, jobEntity);
					return deployOutput;
				}
			};
			taskList.add(task);
		}

		final List<ZETaskResult<String>> resultList = ZE.submitInQueue(taskList);
		final StringBuilder builder = new StringBuilder();
		for (final ZETaskResult<String> zeTaskResult : resultList) {
			final String r = zeTaskResult.get();

			builder.append(r).append("<br/");
		}

		return CR.ok(builder.toString());
	}

	/**
	 * 在节点上是否存在端口号为port的进程
	 *
	 * @param port
	 * @param nodeId
	 * @return
	 *
	 */
	public CR<String> existPortOnNode(final Integer port, final Integer nodeId) {

		if (port == null) {
			return CR.error("port不能为空");
		}
		if (nodeId == null) {
			return CR.error("nodeId不能为空");
		}

		final NodeEntity nodeEntity = this.nodeRepository.findById(nodeId);
		if (nodeEntity == null) {
			return CR.error("节点不存在,id = " + nodeId);
		}

		final String netstat = "ssh root@" + nodeEntity.getIp() + " \"netstat -tln | grep " + port + "\"";
		System.out.println("netstat = " + netstat);
		final String netstatOutput = SVNService.executeLinux("NETSATA" + "@" + nodeId + "@"  + port, netstat);
		System.out.println("netstatOutput = " + netstatOutput);

		final String output = StrUtil.isEmpty(netstatOutput) ?  WEI_YUNXING : YUNXINGZHONG;

		return CR.ok("[" + output + "]" + " - " + nodeId + " - " + nodeEntity.getNickName());
	}

	// FIXME 2023年9月29日 下午10:53:03 zhanghen: TODO 一个主机有多个ip时，怎么判断新增的节点ip是否同一台呢？
	public synchronized CR<Object> createOne(final NewNodeDTO newNodeDTO) {
		LOG.info("开始新增节点,newNodeDTO={}", newNodeDTO);
		final Matcher matcher = IP_PATTERN.matcher(newNodeDTO.getIp());
		if (!matcher.matches()) {
			return CR.error("ip格式错误，请重新输入ip");
		}

		final List<NodeEntity> findByIp = this.nodeRepository.findByIp(newNodeDTO.getIp());
		if (CollUtil.isNotEmpty(findByIp)) {
			return CR.error("ip已存在，请重新输入ip");
		}

		final List<NodeEntity> findByNickName = this.nodeRepository.findByNickName(newNodeDTO.getName());
		if (CollUtil.isNotEmpty(findByNickName)) {
			return CR.error("名称已存在，请重新输入名称");
		}

		final NodeEntity entity = new NodeEntity();
		entity.setIp(newNodeDTO.getIp());
		entity.setNickName(newNodeDTO.getName());
		entity.setStatus(NodeStatusEnum.QI_YONG.getStatus());

		final NodeEntity save = this.nodeRepository.save(entity);

		return CR.ok(save);
	}

	public CR stopNode(final StopNodeDTO stopNodeDTO) {
		LOG.info("开始停用节点,stopNodeDTO={}", stopNodeDTO);

		final Integer nodeId = stopNodeDTO.getNodeId();
		final NodeEntity entity = this.nodeRepository.findById(nodeId);
		if (entity == null) {
			return CR.error("节点不存在,nodeId = " + stopNodeDTO.getNodeId());
		}

		final NodeStatusEnum statusEnum = NodeStatusEnum.valueOfStatus(entity.getStatus());
		if (statusEnum != NodeStatusEnum.QI_YONG) {
			return CR.error("节点非[启用]状态，不可以进行[停用]操作,nodeId = " + stopNodeDTO.getNodeId());
		}

		entity.setStatus(NodeStatusEnum.JIN_YONG.getStatus());

		final NodeEntity update = this.nodeRepository.update(entity);

		return CR.ok(update);
	}

	public CR startNode(final StopNodeDTO stopNodeDTO) {
		LOG.info("开始启用节点,stopNodeDTO={}", stopNodeDTO);

		final Integer nodeId = stopNodeDTO.getNodeId();
		final NodeEntity entity = this.nodeRepository.findById(nodeId);
		if (entity == null) {
			return CR.error("节点不存在,nodeId = " + stopNodeDTO.getNodeId());
		}

		final NodeStatusEnum statusEnum = NodeStatusEnum.valueOfStatus(entity.getStatus());
		if (statusEnum != NodeStatusEnum.JIN_YONG) {
			return CR.error("节点非[启用]状态，不可以进行[停用]操作,nodeId = " + stopNodeDTO.getNodeId());
		}

		entity.setStatus(NodeStatusEnum.QI_YONG.getStatus());

		final NodeEntity update = this.nodeRepository.update(entity);

		return CR.ok(update);
	}


}
