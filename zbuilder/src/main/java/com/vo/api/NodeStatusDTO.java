package com.vo.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年9月30日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NodeStatusDTO {

	private Integer jobId;
	private String jobName;
	private Integer nodeId;
	private String nodeIp;
	private String nodeName;
	private Integer nodeStatus;
	private String nodeStatusString;
}
