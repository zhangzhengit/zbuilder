package com.vo.dto;

import com.vo.validator.ZNotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * 构建
 *
 * @author zhangzhen
 * @date 2023年9月24日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BuildDTO {

	private Integer jobId;

	/**
	 * 	构建备注信息，如：测试
	 */
	private String remark;
}
