package com.vo.enums;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;
import com.vo.enums.MethodEnum;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月23日
 *
 */
@Getter
@AllArgsConstructor
public enum NodeStatusEnum {

	JIN_YONG(1,"禁用"),

	QI_YONG(2,"启用"),;

	private Integer status;
	private String desc;

	private final static ConcurrentMap<Integer, NodeStatusEnum> mapV = Maps.newConcurrentMap();
	static {
		final NodeStatusEnum[] v = values();
		for (final NodeStatusEnum e : v) {
			mapV.put(e.getStatus(), e);
		}

	}

	public static NodeStatusEnum valueOfStatus(final Integer status) {
		return mapV.get(status);
	}

}
