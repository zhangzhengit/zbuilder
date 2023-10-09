package com.vo.repository;

import java.util.List;

import com.vo.ZRepository;
import com.vo.entity.NodeEntity;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月23日
 *
 */
public interface NodeRepository extends ZRepository<NodeEntity, Integer> {

	List<NodeEntity> findByIp(String ip);

	List<NodeEntity> findByNickName(String nickName);
	List<NodeEntity> findByUserName(String userName);

}
