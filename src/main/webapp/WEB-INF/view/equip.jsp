<%@ page language="java" pageEncoding="utf-8"%>
<%  
    response.setHeader("Pragma", "No-cache");
    response.setHeader("Cache-Control", "no-cache");
    response.setDateHeader("Expires", 0);   
%>
<!DOCTYPE html>
<html>
	<head>
		<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
		<meta charset="utf-8" />
		<title>${_systemName}</title>
		<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" />
		
		<link rel="stylesheet" href="${_path}/css/ui.jqgrid.css" />
		<link rel="stylesheet" href="${_staticPath}/resource/bootstrap-3.3.5/css/bootstrap.min.css" />
		
	</head>

	<body class="no-skin">
	
	
		<!-- basic scripts -->

		<!--[if !IE]> -->
		<script type="text/javascript">
			window.jQuery || document.write("<script src='${_staticPath}/assets/js/jquery.js'>"+"<"+"/script>");
		</script>

		<!-- <![endif]-->
		
		<script src="${_staticPath}/assets/js/bootstrap.js"></script>
		<script src="${_path}/js/jquery.jqGrid.js"></script>
		<div class ="jqGrid_wrapper">
	    	<table id ="jqGridList"> </table>
	    	<div id ="jqGridPager"> </div>
		</div>
		
		
		
		
		
		<script type="text/javascript">
			var jqGrid = $("#jqGridList");
	        jqGrid.jqGrid({
	            caption: "用户管理",
	            url: "/User/GetList",
	            mtype: "GET",
	            styleUI: 'Bootstrap',//设置jqgrid的全局样式为bootstrap样式
	            datatype: "json",
	            colNames: ['主键', '登录帐号', '姓名','性别', '邮箱', '电话', '身份证'],
	            colModel: [
	                { name: 'Id', index: 'Id', width: 60, key: true, hidden: true },
	                { name: 'Code', index: 'Code', width: 60 },
	                { name: 'Name', index: 'Name', width: 60 },
	                {
	                    name: 'Gender', index: 'Gender', width: 60,
	                    formatter: function(cellValue, options, rowObject) {
	                        return cellValue == 0 ? "男" : "女";
	                    }//jqgrid自定义格式化
	                },
	                { name: 'Email', index: 'Email', width: 60 },
	                { name: 'Phone', index: 'Phone', width: 60 },
	                { name: 'IdCard', index: 'IdCard', width: 60 }
	            ],
	            viewrecords: true,
	            multiselect: true,
	            rownumbers: true,
	            autowidth: true,
	            height: "100%",
	            rowNum: 20,
	            rownumbers: true, // 显示行号
	            rownumWidth: 35, // the width of the row numbers columns
	            pager: "#jqGridPager",//分页控件的id
	            subGrid: false//是否启用子表格
	        });
	
	        // 设置jqgrid的宽度
	        $(window).bind('resize', function () {
	            var width = $('.jqGrid_wrapper').width();
	            jqGrid.setGridWidth(width);
	        });
		</script>
	</body>
</html>
