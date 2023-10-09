package com.vo.api;

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
public class NewNodeDTO {

	private String name;
	private String ip;
}
