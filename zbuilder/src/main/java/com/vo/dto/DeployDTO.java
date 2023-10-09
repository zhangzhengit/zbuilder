package com.vo.dto;

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

	private String version;
	private Integer hId;

}
