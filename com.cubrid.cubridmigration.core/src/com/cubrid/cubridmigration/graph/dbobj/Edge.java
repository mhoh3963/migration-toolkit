package com.cubrid.cubridmigration.graph.dbobj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.cubrid.cubridmigration.core.dbobject.Column;
import com.cubrid.cubridmigration.core.dbobject.DBObject;

public class Edge extends DBObject {
   
	private static final long serialVersionUID = -547679137789424327L;
	
	public static final int NONE = 0;
    public static final int SECOND_FK_TYPE = 1;
    public static final int INTERMEDIATE_FK_TYPE = 2;
    public static final int JOINTABLE_TYPE = 3;
    public static final int RECURSIVE_TYPE = 4;
    public static final int CUSTOM_TYPE = 5;
    public static final int TWO_WAY_TYPE = 6;
    public static final int JOIN_TWO_WAY_TYPE = 7;
    
    private long oid;
    
	private int id;
	
	//GDB isSelected for check page
	private boolean isSelected;
	
	private String edgeLabel;
	private Vertex startVertex;
	private Vertex endVertex;
	
	private String startVertexName;
	private String endVertexName;
	private String sourceDBObject;

	private boolean hasDateTimeFilter = false;

	String ddl = "-";
	
	private List<Column> columnList = new ArrayList<Column>();
	
	private Map<String, String> fkCol2RefMapping = new TreeMap<String, String>();
	
	private Map<String, String> edgeProperties = new HashMap<String, String>();
    
	private String fkString;
	
	private int edgeType = NONE;
	private String owner;
	
	boolean isHavePKStartVertex = false;
	
	public Edge() {
	}
	
	public Edge(Edge edge) {
		this.oid = edge.getOid();
		this.id = edge.getId();
		this.edgeLabel = edge.getEdgeLabel();
		this.startVertex = edge.getStartVertex();
		this.endVertex = edge.getEndVertex();
		this.startVertexName = edge.getStartVertexName();
		this.endVertexName = edge.getEndVertexName();
		this.columnList = new ArrayList<Column>(edge.getColumnList());
		this.fkCol2RefMapping = new TreeMap<String, String>(edge.getfkCol2RefMapping());
		this.edgeProperties = new HashMap<String, String>(edge.getEdgeProperties());
		this.fkString = edge.getFKString();
		this.owner = edge.getOwner();
		this.setHavePKStartVertex(edge.isHavePKStartVertex);
	}
	
	public List<Column> getColumnList() {
		return columnList;
	}
	public void setColumnList(List<Column> columnList) {
		this.columnList = columnList;
	}
	public void addColumn(Column col) {
		this.columnList.add(col);
	}
	
	public void addColumnAtFirst(Column col) {
		this.columnList.add(0, col);
	}
	
	public Column getColumnbyName(String columnName) {
		for (Column column : columnList) {
			if (column.getName().equals(columnName)) {
				return column;
			}
		}
		return null;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public boolean isSelected() {
		return isSelected;
	}
	public void setSelect(boolean isSelected) {
		this.isSelected = isSelected;
	}
	public String getEdgeLabel() {
		return edgeLabel;
	}
	public void setEdgeLabel(String edgeLabel) {
		this.edgeLabel = edgeLabel;
	}
	public Vertex getStartVertex() {
		return startVertex;
	}
	public void setStartVertex(Vertex startVertex) {
		this.startVertex = startVertex;
	}
	public Vertex getEndVertex() {
		return endVertex;
	}
	public void setEndVertex(Vertex endVertex) {
		this.endVertex = endVertex;
	}
	public String getStartVertexName() {
		return startVertexName;
	}
	public void setStartVertexName(String startVertexName) {
		this.startVertexName = startVertexName;
	}
	public String getEndVertexName() {
		return endVertexName;
	}
	public void setEndVertexName(String endVertexName) {
		this.endVertexName = endVertexName;
	}
	public Map<String, String> getEdgeProperties() {
		return edgeProperties;
	}
	public void setEdgeProperties(Map<String, String> edgeProperties) {
		this.edgeProperties = edgeProperties;
	}
	public void putVertexProperties(String key, String value) {
		this.edgeProperties.put(key, value);
	}
	
	public String getFKString() {
		return fkString;
	}
	public void setFKSring(String fkString) {
		this.fkString = fkString;
	}
    
	public void setEdgeType(int type) {
	    this.edgeType = type;
	}
	
	public int getEdgeType() {
	    return this.edgeType;
	}
	
	public void setOwner(String owner) {
	    this.owner = owner;
	}
	
	public String getOwner() {
	    return this.owner;
	}
	
	public void setHavePKStartVertex(boolean haved) {
	    this.isHavePKStartVertex = haved;
	}
	
	public boolean isHavePKStartVertex() {
	    return this.isHavePKStartVertex;
	}
	
	public void clearFKCol2Ref() {
		fkCol2RefMapping.clear();
	}
	
	public void addFKCol2Ref(String fkColumnName, String refColumName) {
		fkCol2RefMapping.put(fkColumnName, refColumName);
	}
	
	public Map<String, String> getfkCol2RefMapping() {
		return fkCol2RefMapping;
	}
	
	public int getfkCol2RefMappingSize() {
		return fkCol2RefMapping.size();
	}
	
	public List<String> getFKColumnNames() {
		return new ArrayList<String>(fkCol2RefMapping.keySet());
	}
	
	public String getREFColumnNames(String columnName) {
		return fkCol2RefMapping.get(columnName);
	}
	
	public void transColToProp(){
		if (columnList == null) {
			return;
		}
		
		for (Column col : columnList) {
			if (col == null) {
				continue;
			}
			
			edgeProperties.put(col.getName(), col.getDataType());
		}
	}

	public long getOid() {
		return oid;
	}

	public void setOid(long oid) {
		this.oid = oid;
	}
	
	public void removeIDCol() {
		Iterator<Column> iter = columnList.iterator();
		
		while (iter.hasNext()) {
			Column col = iter.next();
			if (col.getDataType().equals("ID")) {
				iter.remove();
			}
		}
	}
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return edgeLabel;
	}

	@Override
	public String getObjType() {
		// TODO Auto-generated method stub
		return getEdgeType() == 3 ? "Join Table Edge" : "Edge";
	}

	@Override
	public String getDDL() {
		// TODO Auto-generated method stub
		return ddl;
	}
	
	public void setSourceDBObject() {
		this.sourceDBObject = getObjType() + " (" + edgeLabel + ", " + startVertexName + " -> " + endVertexName + ")";
	}
	
	public String getSourceDBObject() {
		if (sourceDBObject == null || sourceDBObject.isEmpty()) {
			setSourceDBObject();
		}
		
		return sourceDBObject;
	}
	
	public boolean hasDateTimeFilter() {
		return hasDateTimeFilter;
	}

	public void setHasDateTimeFilter(boolean hasTimeFilter) {
		if (hasTimeFilter == false) {
			removeDateTimeFilter();
		}
		
		this.hasDateTimeFilter = hasTimeFilter;
	}	
	
	private void removeDateTimeFilter() {
		for (Column col : getColumnList()) {
			if (col.isConditionColumn() == true) {
				col.setConditionColumn(false);
				break;
			}
		}
	}
	
	public Column getConditionColumn() {
		for (Column col : columnList) {
			if (col.isConditionColumn()) {
				return col;
			}
		}
		
		return null;
	}
}
