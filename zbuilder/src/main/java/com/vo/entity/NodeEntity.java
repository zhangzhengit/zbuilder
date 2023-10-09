package com.vo.entity;

import com.vo.ZID;
import com.vo.anno.ZEntity;
import com.vo.enums.NodeStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月23日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZEntity(tableName = "node")
public class NodeEntity {

	@ZID
	private Integer id;
	private String ip;
	private String userName;
	private String password;
	private String nickName;

	/**
	 * @see NodeStatusEnum
	 */
	private Integer status;

}
