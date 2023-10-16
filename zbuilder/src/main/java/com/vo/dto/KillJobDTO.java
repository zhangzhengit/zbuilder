package com.vo.dto;

import com.vo.validator.ZNotEmtpy;
import com.vo.validator.ZNotNull;

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
public class KillJobDTO {

	@ZNotNull
	private Integer nodeId;

	@ZNotNull
	private Integer jobId;

}
