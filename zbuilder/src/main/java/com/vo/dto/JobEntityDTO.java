package com.vo.dto;

import java.util.List;

import com.vo.entity.JobEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月26日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobEntityDTO extends JobEntity {

	private String statusString;

	/**
	 * 多个节点名称，多个用,隔开
	 */
	private String nodeNickName;

}
