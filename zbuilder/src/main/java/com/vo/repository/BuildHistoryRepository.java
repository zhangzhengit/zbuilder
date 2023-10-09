package com.vo.repository;

import java.util.List;

import com.vo.ZRepository;
import com.vo.entity.BuildHistoryEntity;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月26日
 *
 */
public interface BuildHistoryRepository extends ZRepository<BuildHistoryEntity, Integer> {

	List<BuildHistoryEntity> findByJobId(Integer jobId);
	List<BuildHistoryEntity> findByUuid(final String uuid);

}
