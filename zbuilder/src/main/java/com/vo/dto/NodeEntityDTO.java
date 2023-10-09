package com.vo.dto;

import java.security.KeyStore.PrivateKeyEntry;

import com.vo.entity.NodeEntity;

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
public class NodeEntityDTO extends NodeEntity {

	private String statusString;

}
