/*
 * Copyright (C) 2009 Search Solution Corporation. All rights reserved by Search Solution. 
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met: 
 *
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer. 
 *
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution. 
 *
 * - Neither the name of the <ORGANIZATION> nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software without 
 *   specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY 
 * OF SUCH DAMAGE. 
 *
 */
package com.cubrid.cubridmigration.core.trans;

import com.cubrid.cubridmigration.core.dbtype.DatabaseType;
import com.cubrid.cubridmigration.cubrid.trans.ToCUBRIDDataConverterFacade;
import com.cubrid.cubridmigration.graph.trans.CUBRID2Neo4jTranformHelper;
import com.cubrid.cubridmigration.graph.trans.CUBRID2TurboTranformHelper;
import com.cubrid.cubridmigration.graph.trans.CUBRIDToTurboDataTypeMappingHelper;
import com.cubrid.cubridmigration.graph.trans.Neo4j2CUBRIDTranformHelper;
import com.cubrid.cubridmigration.graph.trans.Neo4j2TiberoTranformHelper;
import com.cubrid.cubridmigration.graph.trans.Neo4jDataTypeMappingHelper;
import com.cubrid.cubridmigration.graph.trans.Neo4jToCUBRIDDataTypeMappingHelper;
import com.cubrid.cubridmigration.graph.trans.Neo4jToTiberoDataTypeMappingHelper;
import com.cubrid.cubridmigration.graph.trans.Tibero2Neo4jTranformHelper;
import com.cubrid.cubridmigration.graph.trans.Tibero2TurboTranformHelper;
import com.cubrid.cubridmigration.graph.trans.TiberoToNeo4jDataTypeMappingHelper;
import com.cubrid.cubridmigration.graph.trans.TiberoToTurboDataTypeMappingHelper;
import com.cubrid.cubridmigration.graph.trans.ToNeo4jDataConverterFacade;
import com.cubrid.cubridmigration.graph.trans.ToTiberoDataConverterFacade;
import com.cubrid.cubridmigration.graph.trans.ToTurboDataConverterFacade;
import com.cubrid.cubridmigration.graph.trans.Turbo2CUBRIDTransformHelper;
import com.cubrid.cubridmigration.graph.trans.Turbo2TiberoTransformHelper;
import com.cubrid.cubridmigration.graph.trans.TurboToCUBRIDDataTypeMappingHelper;
import com.cubrid.cubridmigration.graph.trans.TurboToTiberoDataTypeMappingHelper;

/**
 * MigrationTransFactory will return the DBTransform instance by input source
 * database type and target database type.
 * 
 * @author Kevin Cao
 * @version 1.0 - 2013-11-14 created by Kevin Cao
 */
public class MigrationTransFactory {
	private static final CUBRID2Neo4jTranformHelper CUBRID2NEO4J_TRANSFORM_HELPER = new CUBRID2Neo4jTranformHelper(
			new Neo4jDataTypeMappingHelper(),
			ToNeo4jDataConverterFacade.getInstance());

	private static final CUBRID2TurboTranformHelper CUBRID2TURBO_TRANSFORM_HELPER = new CUBRID2TurboTranformHelper(
			new CUBRIDToTurboDataTypeMappingHelper(),
			ToTurboDataConverterFacade.getInstance());

	private static final Tibero2Neo4jTranformHelper TIBERO2NEO4J_TRANSFORM_HELPER = new Tibero2Neo4jTranformHelper(
			new TiberoToNeo4jDataTypeMappingHelper(),
			ToNeo4jDataConverterFacade.getInstance());

	private static final Tibero2TurboTranformHelper TIBERO2TURBO_TRANFORM_HELPER = new Tibero2TurboTranformHelper(
			new TiberoToTurboDataTypeMappingHelper(),
			ToTurboDataConverterFacade.getInstance());

	private static final Neo4j2CUBRIDTranformHelper NEO4J2CUBRID_TRANSFORM_HELPER = new Neo4j2CUBRIDTranformHelper(
			new Neo4jToCUBRIDDataTypeMappingHelper(),
			ToCUBRIDDataConverterFacade.getIntance());

	private static final Neo4j2TiberoTranformHelper NEO4J2TIBERO_TRANSFORM_HELPER = new Neo4j2TiberoTranformHelper(
			new Neo4jToTiberoDataTypeMappingHelper(),
			ToTiberoDataConverterFacade.getInstance());

	private static final Turbo2CUBRIDTransformHelper TURBO2CUBRID_TRANSFORM_HELPER = new Turbo2CUBRIDTransformHelper(
			new TurboToCUBRIDDataTypeMappingHelper(),
			ToCUBRIDDataConverterFacade.getIntance());
	
	private static final Turbo2TiberoTransformHelper TURBO2TIBERO_TRANSFORM_HELPER = new Turbo2TiberoTransformHelper (
			new TurboToTiberoDataTypeMappingHelper(),
			ToTiberoDataConverterFacade.getInstance());

	/**
	 * getTransformHelper of source to target migration
	 * 
	 * @param srcDT DatabaseType of source
	 * @param tarDT DatabaseType of target
	 * @return DBTranformHelper
	 */
	public static DBTransformHelper getTransformHelper(DatabaseType srcDT,
			DatabaseType tarDT) {
		if (srcDT.getID() == DatabaseType.CUBRID.getID()) {
			if (tarDT.getID() == DatabaseType.NEO4J.getID()) {
				return CUBRID2NEO4J_TRANSFORM_HELPER;
			} else {
				return CUBRID2TURBO_TRANSFORM_HELPER;
			}
		} else if (srcDT.getID() == DatabaseType.TIBERO.getID()) {
			if (tarDT.getID() == DatabaseType.NEO4J.getID()) {
				return TIBERO2NEO4J_TRANSFORM_HELPER;
			} else {
				return TIBERO2TURBO_TRANFORM_HELPER;
			}
		} else if (srcDT.getID() == DatabaseType.NEO4J.getID()) {
			if (tarDT.getID() == DatabaseType.TIBERO.getID()) {
				return NEO4J2TIBERO_TRANSFORM_HELPER;
			} else {
				return NEO4J2CUBRID_TRANSFORM_HELPER;
			}
		} else if (srcDT.getID() == DatabaseType.TURBO.getID()) {
			if (tarDT.getID() == DatabaseType.CUBRID.getID()) {
				return TURBO2CUBRID_TRANSFORM_HELPER;
			} else {
				return TURBO2TIBERO_TRANSFORM_HELPER;
			}
		}
		throw new IllegalArgumentException("Can't support migration type.");
	}
}
