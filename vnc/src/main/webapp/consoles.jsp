<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

<link href="css/bootstrap.min.css" rel="stylesheet">

<title>Consoles</title>

<script type="text/javascript">
function access(lable,session) {
	window.open("<%=request.getContextPath()%>/vnc/vmation?vmlable="+ lable +"&xensession=" + session);
}
</script>

</head>

<body>
	<legend>XenServer All VM</legend>
	
	<div class="csPanelTable">
		<div class="csPanelTablecont" id="csPanelTablecont">
			<table class="table table-bordered table-striped table-hover">
				<thead>
					<tr>
						<th width="6%">虚拟机名称</th>
						<th width="6%">会话ID</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach items="${lables}" var="lable" varStatus="index">
						<tr>
							<td width="6%"><a onclick="access('${lable}', '${xensession}')">${lable}</a></td>
							<td width="6%">${xensession}</td>
						</tr>
					</c:forEach>
				</tbody>
			</table>
		</div>

	</div>

</body>
</html>