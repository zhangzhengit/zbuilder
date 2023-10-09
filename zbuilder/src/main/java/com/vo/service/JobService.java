package com.vo.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ser.impl.IndexedListSerializer;
import com.google.common.collect.Lists;

import com.vo.anno.ZAutowired;
import com.vo.anno.ZComponent;
import com.vo.entity.JobEntity;
import com.vo.entity.NodeEntity;
import com.vo.enums.JobStatusEnum;
import com.vo.repository.JobRepository;
import com.vo.repository.NodeRepository;
import com.vo.repository.NodeService;
import com.votool.common.CR;
import com.votool.ze.AbstractZETask;
import com.votool.ze.ZE;
import com.votool.ze.ZES;
import com.votool.ze.ZETaskResult;

import lombok.val;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月29日
 *
 */
@ZComponent
public class JobService {

	private final static ZE ZE = ZES.newZE();

	@ZAutowired
	private NodeRepository nodeRepository;
	@ZAutowired
	private JobRepository jobRepository;
	@ZAutowired
	private NodeService nodeService;

	public List<Integer> parseNodeIdArray(final JobEntity entity) {
		final String id = entity.getTargetNodeId();
		final String[] a = id.split(",");
		final List<Integer> collect = Lists.newArrayList(a).stream().map(s -> Integer.valueOf(s))
				.collect(Collectors.toList());
		return collect;
	}

	public CR<String> checkNodePort(final Integer id) {
		System.out.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
				+ "JobService.checkNodePort()");

		final JobEntity entity = this.jobRepository.findById(id);

		final String targetNodeId = entity.getTargetNodeId();
		final ArrayList<String> nodeIdList = Lists.newArrayList(targetNodeId.split(","));
		final List<Integer> nidlist = nodeIdList.stream().map(s -> Integer.valueOf(s)).collect(Collectors.toList());
		final List<NodeEntity> nodeList = this.nodeRepository.findByIdIn(nidlist);

		final List<AbstractZETask<CR<String>>> taskList = Lists.newArrayList();
		for (final NodeEntity nodeEntity : nodeList) {
			final AbstractZETask<CR<String>> task = new AbstractZETask<CR<String>>() {

				@Override
				public CR<String> call() {
					return JobService.this.nodeService.existPortOnNode(entity.getAppPort(), nodeEntity.getId());
				}
			};
			taskList.add(task);
		}

		final List<ZETaskResult<CR<String>>> resultList = ZE.submitInQueue(taskList);

		final StringBuilder builder = new StringBuilder();
		for (final ZETaskResult<CR<String>> result : resultList) {
			final CR<String> cr = result.get();
			System.out.println("cr = " + cr);
			builder.append(cr.getData()).append("<br/>");
		}
		return CR.ok(builder.toString());
	}

	public List<NodeEntity> findNodeInfoById(final Integer id) {
		final JobEntity entity = this.jobRepository.findById(id);
		if (entity == null) {
			return Collections.emptyList();
		}

		final String nodeID = entity.getTargetNodeId();
		final String[] idA = nodeID.split(",");
		final Set<Integer> idSet = Lists.newArrayList(idA).stream().map(i -> Integer.valueOf(i))
				.collect(Collectors.toSet());
		final List<NodeEntity> nodeList = this.nodeRepository.findByIdIn(Lists.newArrayList(idSet));

		return nodeList;
	}

	/**
	 * 修改job状态
	 *
	 * @param id
	 * @param statusEnum
	 * @return
	 *
	 */
	public JobEntity updateStatus(final Integer id, final JobStatusEnum statusEnum) {

		final JobEntity jobEntity = this.jobRepository.findById(id);
		if (jobEntity == null) {
			throw new IllegalArgumentException("job不存在");
		}

		jobEntity.setStatus(statusEnum.getStatus());

		final JobEntity update = this.jobRepository.update(jobEntity);
		return update;
	}

	public JobStatusEnum getStatusById(final Integer id) {
		final JobEntity jobEntity = this.jobRepository.findById(id);
		if (jobEntity == null) {
			throw new IllegalArgumentException("job不存在");
		}

		return JobStatusEnum.valueOfStatus(jobEntity.getStatus());
	}
}
