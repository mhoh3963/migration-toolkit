package com.cubrid.cubridmigration.graph.export;

import com.cubrid.cubridmigration.core.dbobject.PK;
import com.cubrid.cubridmigration.core.dbtype.DatabaseType;
import com.cubrid.cubridmigration.core.export.DBExportHelper;

public class GraphExportHelper extends DBExportHelper {

	//GDB GraphExportHelper
	
	public DatabaseType getDBType() {
		return null;
	}

	@Override
	protected String getQuotedObjName(String objectName) {
		return null;
	}

	@Override
	public String getPagedSelectSQL(String sql, long pageSize,
			long exportedRecords, PK pk) {
		return null;
	}

}
