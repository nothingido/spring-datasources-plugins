package com.plugin.datasources;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.plugin.datasources.xml.DataSource;
import com.plugin.datasources.xml.DataSourceJNDI;
import com.plugin.datasources.xml.DataSources;
import com.plugin.datasources.xml.XmlIO;

/**
 * 负责重新读取数据源的配置文件，动态更新多数据源
 * 
 * @author 1635
 *
 */
public class MultipleDataSourceManager {
	private final Logger log = LoggerFactory.getLogger(MultipleDataSourceManager.class);

	private String dataSourceFilename = "spring-datasources-plugin.xml";// 默认
	private long lastModifyed = Long.MIN_VALUE;

	private Map<Object, Object> dataSourceMap;
	
	private String defaultDataSource;

	public MultipleDataSourceManager() {
		log.info("初始化多数据源");
		loadDataSourcesFromXML(true);
	}

	public String getDefaultDataSource() {
		return defaultDataSource;
	}

	public void setDefaultDataSource(String defaultDataSource) {
		this.defaultDataSource = defaultDataSource;
	}

	public String getDataSourceFilename() {
		return dataSourceFilename;
	}

	public void setDataSourceFilename(String dataSourceFilename) {
		if (dataSourceFilename == null || dataSourceFilename.length() == 0) {
			return;
		}
		this.dataSourceFilename = dataSourceFilename;
	}

	public Map<Object, Object> getDataSourceMap() {
		return this.dataSourceMap;
	}

	/**
	 * 提供检查更新多数据源的接口
	 */
	public void loadDataSourcesFromXML(boolean init) {
		String path = this.getClass().getResource("/").getPath() + dataSourceFilename;
		log.info("数据源路径：" + path);
		if (init && isExists() || isChanged()) {
			DataSources dss = XmlIO.unmarshal(DataSources.class, path);

			Map<Object, Object> dataSourceMapTemp = new HashMap<Object, Object>();
			for (DataSource ds : dss.getDataSource()) {
				BasicDataSource basicDataSource = new BasicDataSource();
				basicDataSource.setDriverClassName(ds.getDriverClassName());
				basicDataSource.setUrl(ds.getUrl());
				basicDataSource.setUsername(ds.getUserName());
				basicDataSource.setPassword(ds.getPassword());
				basicDataSource.setTestWhileIdle(true);

				dataSourceMapTemp.put(ds.getId(), basicDataSource);
			}

			for (DataSourceJNDI ds : dss.getDataSourceJNDI()) {
				dataSourceMapTemp.put(ds.getId(), ds.getJndiName());
			}
			
			/*for (DataSourceJNDI ds : dss.getDataSourceJNDI()) {
				try {
					String env = "java:comp/env/" + ds.getJndiName();
					Context ctx = new InitialContext();
					javax.sql.DataSource dsTemp = (javax.sql.DataSource) ctx.lookup(env);

					dataSourceMapTemp.put(ds.getId(), dsTemp);
				} catch (NamingException e) {
					e.printStackTrace();
				}
			}*/
			this.dataSourceMap = dataSourceMapTemp;
			this.defaultDataSource = dss.getDefaultDataSource();
			// 保存最后的修改时间戳
			this.lastModifyed = new File(path).lastModified();
		}
	}

	public boolean isChanged() {
		String path = this.getClass().getResource("/").getPath() + dataSourceFilename;
		File f = new File(path);
		if (isExists() && f.lastModified() > this.lastModifyed) {
			return true;
		} else {
			return false;
		}

	}

	public boolean isExists() {
		String path = this.getClass().getResource("/").getPath() + dataSourceFilename;
		File f = new File(path);

		if (f.exists()) {
			return true;
		} else {
			log.info("数据源的配置文件不存在：" + path);
			return false;
		}
	}
}
