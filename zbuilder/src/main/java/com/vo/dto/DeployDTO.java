package com.vo.dto;

import com.vo.validator.ZNotEmtpy;
import com.vo.validator.ZNotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 部署用的DTO
 *
 * @author zhangzhen
 * @date 2023年9月26日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeployDTO {

	@ZNotEmtpy
	private String version;

	@ZNotNull
	private Integer hId;

}
