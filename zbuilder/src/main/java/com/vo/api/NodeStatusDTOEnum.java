package com.vo.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月30日
 *
 */
@Getter
@AllArgsConstructor
public enum NodeStatusDTOEnum {

	YUNXINGZHONG(1),

	WEI_YUNXING(2),
	;

	private Integer status;
}
