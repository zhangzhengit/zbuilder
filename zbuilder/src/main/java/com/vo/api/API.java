package com.vo.api;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.aspectj.weaver.reflect.ReflectionBasedReferenceTypeDelegate;
import org.codehaus.groovy.transform.tailrec.ReturnAdderForClosures;

import com.fasterxml.jackson.databind.ext.DOMDeserializer.NodeDeserializer;
import com.google.common.collect.Lists;
import com.vo.anno.ZAutowired;
import com.vo.anno.ZController;
import com.vo.anno.ZRequestBody;
import com.vo.core.ZContext;
import com.vo.dto.BuildDTO;
import com.vo.dto.CheckNodePortDTO;
import com.vo.dto.DeployDTO;
import com.vo.dto.JobEntityDTO;
import com.vo.dto.KillJobDTO;
import com.vo.dto.NodeEntityDTO;
import com.vo.dto.StartJobDTO;
import com.vo.entity.BuildHistoryEntity;
import com.vo.entity.JobEntity;
import com.vo.entity.NodeEntity;
import com.vo.enums.JobStatusEnum;
import com.vo.enums.MethodEnum;
import com.vo.enums.NodeStatusEnum;
import com.vo.http.ZHtml;
import com.vo.http.ZRequestMapping;
import com.vo.http.ZRequestParam;
import com.vo.repository.BuildHistoryRepository;
import com.vo.repository.JobRepository;
import com.vo.repository.NodeRepository;
import com.vo.repository.NodeService;
import com.vo.service.BuildHistoryService;
import com.vo.service.JobService;
import com.vo.service.SVNService;
import com.vo.template.ZModel;
import com.votool.common.CR;
import com.votool.ze.ZE;
import com.votool.ze.ZES;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月23日
 *
 */
@ZController
public class API {
	ZE ze = ZES.newZE();

	@ZAutowired
	private BuildHistoryService buildHistoryService;
	@ZAutowired
	private BuildHistoryRepository buildHistoryRepository;
	@ZAutowired
	private JobRepository jobRepository;
	@ZAutowired
	private NodeRepository nodeRepository;
	@ZAutowired
	private NodeService nodeService;
	@ZAutowired
	private JobService jobService;

	@ZRequestMapping(mapping = { "/" }, qps = 5)
	@ZHtml
	public String index(final ZModel model) {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "API.index()");


		// job列表
		final List<JobEntity> jobList = this.jobRepository.findAll();
		final List<JobEntityDTO> dtoList = jobList.parallelStream().map(j -> {
			final JobEntityDTO dto = BeanUtil.copyProperties(j, JobEntityDTO.class);
			dto.setStatusString(JobStatusEnum.valueOfStatus(j.getStatus()).getDesc());

//			final String id = j.getTargetNodeId();
//			final List<Integer> collect = Lists.newArrayList(id.split(",")).stream().map(Integer::valueOf).collect(Collectors.toList());
//			final List<NodeEntity> nodeLis2t = this.nodeRepository.findByIdIn(collect);
//			final String collect2 = nodeLis2t.stream().map(e -> e.getId() + "-" + e.getNickName()).collect(Collectors.joining("<br/>"));
//			final String collect2 = nodeLis2t.stream().map(e -> e.getId() + "-" + e.getNickName()).collect(Collectors.joining(SVNService.NEW_LINE));
//			dto.setNodeNickName(collect2);

			final CR<String> checkNodePort = this.jobService.checkNodePort(j.getId());
			final String checkNodePortResultString = checkNodePort.getData();
			dto.setNodeNickName(checkNodePortResultString);

			return dto;
		}).collect(Collectors.toList());


		System.out.println("jobList .size = " + dtoList.size());
		model.set("jobList", dtoList);

		// 节点列表
		final List<NodeEntity> nlist = this.nodeRepository.findAll();
		System.out.println("nlist.size = " + nlist.size());

		final List<NodeEntityDTO> nodeList = nlist.stream().map(n -> {
			final NodeEntityDTO r = BeanUtil.copyProperties(n, NodeEntityDTO.class);
			final NodeStatusEnum valueOfStatus = NodeStatusEnum.valueOfStatus(n.getStatus());
			r.setStatusString(valueOfStatus.getDesc());
			return r;
		}).collect(Collectors.toList());

		model.set("nodeList", nodeList);
		model.set("refreshTime", new Date());

		return "html/index.html";
	}


	@ZRequestMapping(mapping = { "/toNewNode" }, qps = 1)
	@ZHtml
	public String toNewNode(final ZModel model) {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "API.toNewNode()");

		return "html/newNode.html";
	}

	@ZRequestMapping(mapping = { "/newNode" }, qps = 1, method = MethodEnum.POST)
	public CR newNode(@ZRequestBody final NewNodeDTO newNodeDTO) {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "API.newNode()");

		final CR<Object> cr = this.nodeService.createOne(newNodeDTO);
		return cr;
	}

	@ZRequestMapping(mapping = { "/build" }, qps = 5)
	@ZHtml
	public String build(final ZModel model, @ZRequestParam final Integer id) {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "API.build()");


		final JobEntity jobEntity = this.jobRepository.findById(id);
		model.set("jobEntity", jobEntity);

		final List<BuildHistoryEntity> historyList = this.buildHistoryRepository.findByJobId(jobEntity.getId());
		Comparator.comparing(BuildHistoryEntity::getId);
		final List<BuildHistoryEntity> reverse = ListUtil.reverse(historyList);
		model.set("historyList", reverse);


		return "html/build.html";
	}

	@ZRequestMapping(mapping = { "/buildstart" }, qps = 5, method = MethodEnum.POST)
	public CR buildstart(@ZRequestBody final BuildDTO buildDTO) {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "API.buildstart()");

		System.out.println("buildDTO = " + buildDTO);

		final CR<Object> cr = BuildService.build(buildDTO);

		return cr;
	}

	@ZRequestMapping(mapping = { "/tobuildResult" }, qps = 5)
	@ZHtml
	public String tobuildResult(final ZModel model, @ZRequestParam final String uuid) {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "API.tobuildResult()");

		model.set("uuid", uuid);
		return "html/buildResult.html";
	}

	@ZRequestMapping(mapping = { "/getBuildInfo" }, qps = 13520)
	public CR getBuildInfo(@ZRequestParam final String uuid) {

//		if (Boolean.TRUE.equals(BuildService.MAP_DONE.get(uuid))) {
//			return CR.error(SVNService.BUILD_END);
//		}

		final String output = BuildService.getOutput(uuid);
		if (SVNService.BUILD_END.equals(output)) {
			return CR.error(SVNService.BUILD_END);
		}

		return CR.ok(output);
	}

	@ZRequestMapping(mapping = { "/todeployResult" }, qps = 5)
	@ZHtml
	public String todeployResult(final ZModel model, @ZRequestParam final String uuid) {
		System.out.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
				+ "API.todeployResult()");

		// FIXME 2023年9月24日 下午7:25:58 zhanghen: 写这里，先加一个页面
		model.set("uuid", uuid);
		return "html/buildResult.html";
	}

	@ZRequestMapping(mapping = { "/deploy" }, qps = 1, method = MethodEnum.POST)
	public CR deploy(@ZRequestBody final DeployDTO deployDTO) {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "API.deploy()");

		final CR<Object> cr = BuildService.deploy(deployDTO);

		return cr;
	}

	/**
	 * 查看一次构建的控制台输出
	 *
	 * @param model
	 * @param uuid
	 * @return
	 *
	 */
	@ZRequestMapping(mapping = { "/showConsole" }, qps = 5)
	@ZHtml
	public String showConsole(final ZModel model, @ZRequestParam final Integer bhId) {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "API.showConsole()");

		// FIXME 2023年9月24日 下午7:25:58 zhanghen: 写这里，先加一个页面
		model.set("bhId", bhId);

		final BuildHistoryEntity entity = this.buildHistoryRepository.findById(bhId);
		if (entity == null) {
			final BuildHistoryEntity out = new BuildHistoryEntity();
			out.setOutput("无此构建信息");
			model.set("consoleOutput", out);
		} else {
			model.set("consoleOutput", entity);
		}


		return "html/consoleOutput.html";
	}

	@ZRequestMapping(mapping = { "/checkNodePort" }, qps = 1,method = MethodEnum.POST)
	public CR checkNodePort(@ZRequestBody final CheckNodePortDTO checkNodePortDTO) {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "API.checkNodePort()");
		// FIXME 2023年9月30日 上午12:00:53 zhanghen: 改为在index.html列表里直接展示 每个节点的job执行状态（有点耗时）
		final CR<String> checkNodePort = this.jobService.checkNodePort(checkNodePortDTO.getJobId());
		return checkNodePort;
	}


	@ZRequestMapping(mapping = { "/toJobDetail" }, qps = 2)
	@ZHtml
	public String toJobDetail(final ZModel model, @ZRequestParam final Integer jobId) {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "API.toJobDetail()");

		final JobEntity entity = this.jobRepository.findById(jobId);
		model.set("jobEntity", entity);

		final List<Integer> nodeIdList = this.jobService.parseNodeIdArray(entity);
		final ArrayList<Object> nodeStatusList = Lists.newArrayList();
		for (final Integer nodeId : nodeIdList) {
			final NodeStatusDTO statusDTO = new NodeStatusDTO();
			statusDTO.setJobId(entity.getId());
			statusDTO.setJobName(entity.getProjectName());
			final CR<String> nodeStatus = this.nodeService.existPortOnNode(entity.getAppPort(), nodeId);
			statusDTO.setNodeId(nodeId);
			final NodeEntity nodeEntity = this.nodeRepository.findById(nodeId);
			statusDTO.setNodeName(nodeEntity.getNickName());
			statusDTO.setNodeIp(nodeEntity.getIp());
			statusDTO.setNodeStatusString(nodeStatus.getData());
			if (nodeStatus.getData().contains(NodeService.YUNXINGZHONG)) {
				statusDTO.setNodeStatus(NodeStatusDTOEnum.YUNXINGZHONG.getStatus());
			} else {
				statusDTO.setNodeStatus(NodeStatusDTOEnum.WEI_YUNXING.getStatus());
			}

			nodeStatusList.add(statusDTO);
		}

		model.set("nodeStatusList", nodeStatusList);

		final CR<String> checkNodePort = this.jobService.checkNodePort(jobId);
		final String checkNodePortResultString = checkNodePort.getData();
		model.set("nodeStatus", checkNodePortResultString);

		final List<BuildHistoryEntity> historyList = this.buildHistoryRepository.findByJobId(jobId);
		final List<BuildHistoryEntity> reverse = ListUtil.reverse(historyList);
		model.set("historyList", reverse);

		return "html/jobDetail.html";
	}

	@ZRequestMapping(mapping = { "/killJob" }, qps = 1, method = MethodEnum.POST)
	public CR killJob(@ZRequestBody final KillJobDTO killJobDTO) {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "API.killJob()");

		final CR<Object> cr = this.nodeService.killJob(killJobDTO);
		return cr;
	}

	@ZRequestMapping(mapping = { "/startJob" }, qps = 1, method = MethodEnum.POST)
	public CR startJob(@ZRequestBody final StartJobDTO startJobDTO) {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "API.startJob()");

		final CR<String> cr = this.nodeService.startJob(startJobDTO);
		return cr;
	}

	@ZRequestMapping(mapping = { "/startJobAllNode" }, qps = 1, method = MethodEnum.POST)
	public CR startJobAllNode(@ZRequestBody final StartJobDTO startJobDTO) {
		System.out.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
				+ "API.startJobAllNode()");

		final CR<String> cr = this.nodeService.startJobAllNode(startJobDTO);
		return cr;
	}

	@ZRequestMapping(mapping = { "/killJobAllNode" }, qps = 1, method = MethodEnum.POST)
	public CR killJobAllNode(@ZRequestBody final KillJobDTO killJobDTO) {
		System.out.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
				+ "API.killJobAllNode()");

		final CR<Object> cr = this.nodeService.killJobAllNode(killJobDTO);
		return cr;
	}

	/**
	 * 停用节点
	 *
	 * @param stopNodeDTO
	 * @return
	 */
	@ZRequestMapping(mapping = { "/stopNode" }, qps = 1, method = MethodEnum.POST)
	public CR stopNode(@ZRequestBody final StopNodeDTO stopNodeDTO) {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "API.stopNode()");

		return this.nodeService.stopNode(stopNodeDTO);
	}
	@ZRequestMapping(mapping = { "/startNode" }, qps = 1, method = MethodEnum.POST)
	public CR startNode(@ZRequestBody final StopNodeDTO stopNodeDTO) {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "API.startNode()");

		return this.nodeService.startNode(stopNodeDTO);
	}

	@ZRequestMapping(mapping = { "/toNewJob" }, qps = 1)
	@ZHtml
	public String toNewJob(final ZModel model) {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "API.toNewJob()");


		return "html/newJob.html";
	}


}
