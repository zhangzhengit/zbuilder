package com.vo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年10月11日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WSDTO {

	/**
	 * 区分客户端的唯一ID
	 */
	private String uuid;

	/**
	 * 通信消息
	 */
	private String message;

}
