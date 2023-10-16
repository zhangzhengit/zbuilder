package com.vo.api;

import com.vo.validator.ZNotEmtpy;
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
public class NewNodeDTO {

	/**
	 * 节点名称
	 */
	@ZNotEmtpy
	private String nickName;

	/**
	 * 连接此节点的用户名
	 */
	@ZNotEmtpy
	private String userName;
	@ZNotEmtpy
	private String remark;
	@ZNotEmtpy
	private String ip;
}
