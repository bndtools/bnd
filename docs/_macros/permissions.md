---
layout: default
class: Builder
title: permissions (';' ( 'packages' | 'admin' | 'permissions' ) )+
summary: A file in the format for the OSGi permissions resource.
---






	public String _permissions(String args[]) {
		StringBuilder sb = new StringBuilder();

		for (String arg : args) {
			if ("packages".equals(arg) || "all".equals(arg)) {
				for (PackageRef imp : getImports().keySet()) {
					if (!imp.isJava()) {
						sb.append("(org.osgi.framework.PackagePermission \"");
						sb.append(imp);
						sb.append("\" \"import\")\r\n");
					}
				}
				for (PackageRef exp : getExports().keySet()) {
					sb.append("(org.osgi.framework.PackagePermission \"");
					sb.append(exp);
					sb.append("\" \"export\")\r\n");
				}
			} else if ("admin".equals(arg) || "all".equals(arg)) {
				sb.append("(org.osgi.framework.AdminPermission)");
			} else if ("permissions".equals(arg))
				;
			else
				error("Invalid option in ${permissions}: %s", arg);
		}
		return sb.toString();
	}

