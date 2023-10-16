package com.vo.api;

import com.vo.core.ZValidated;
import com.vo.validator.ZNotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年10月4日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StopNodeDTO {

	@ZNotNull
	private Integer nodeId;

}
