package com.vo.enums;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月26日
 *
 */
@Getter
@AllArgsConstructor
public enum JobStatusEnum {

//	1-构建中 2-部署中 3-空闲无动作执行

	BUILD(1, "构建中"),

	DEPLOY(2, "部署中"),

	IDLE(3, "空闲无动作执行"),;

	private Integer status;
	private String desc;

	private final static ConcurrentMap<Integer, JobStatusEnum> mapV = Maps.newConcurrentMap();
	static {
		final JobStatusEnum[] v = values();
		for (final JobStatusEnum e : v) {
			mapV.put(e.getStatus(), e);
		}

	}

	public static JobStatusEnum valueOfStatus(final Integer status) {
		return mapV.get(status);
	}
}
