package com.vo.dto;

import com.vo.validator.ZNotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月29日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckNodePortDTO {

	@ZNotNull
	private Integer jobId;

}
