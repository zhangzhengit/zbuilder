package com.vo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年10月2日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StartJobDTO {
	private Integer nodeId;
	private Integer jobId;
}
