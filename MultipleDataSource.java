package com.plugin.datasources;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;


/**
 *  多数据源实现
 * @author 1635
 *
 */
public class MultipleDataSource extends AbstractRoutingDataSource {
	private final Logger log = LoggerFactory.getLogger(MultipleDataSource.class);
	
	private MultipleDataSourceManager multipleDataSourceManager;
	private Object defaultTargetDataSource;
	private Map<Object, Object> targetDataSourcesSpring;
	private Map<Object, Object> targetDataSourcesPlugin;
	
	public void setDefaultTargetDataSource(Object defaultTargetDataSource) {
		this.defaultTargetDataSource = defaultTargetDataSource;
		super.setDefaultTargetDataSource(defaultTargetDataSource);
	}
	
	public void setDataSourceManager(MultipleDataSourceManager multipleDataSourceManager) {
		this.multipleDataSourceManager = multipleDataSourceManager;
		this.setTargetDataSources(this.targetDataSourcesSpring);
	}

	@Override
    protected Object determineCurrentLookupKey() {
		this.multipleDataSourceManager.loadDataSourcesFromXML(false);
		if(this.multipleDataSourceManager.isChanged()){
			log.info("更新数据源");
			this.setTargetDataSources(this.targetDataSourcesSpring);
		}
		
		Object id = DataSourceContextHolder.getDataSourceID();
		if(id == null){
			log.info("数据源为空，将采用插件配置的默认数据源！");
			id = this.multipleDataSourceManager.getDefaultDataSource();
			
			if(id == null){
				log.info("插件配置的默认数据为空，将采用Spring配置的默认数据源！");
				//id = this.defaultTargetDataSource;//内部会判断，然后应用
				if(this.defaultTargetDataSource == null){
					log.error("Spring没有配置默认数据源！");
					log.error("没有应用任何数据源！");
				}
			}
		}else{
			log.debug("应用插件配置数据源：" + id);
		}
		
		log.info("数据源选择：" + id);
		
        return id;
    }
    
	/**
	 * 只调用一次
	 */
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
		this.targetDataSourcesPlugin = this.multipleDataSourceManager.getDataSourceMap();
        this.targetDataSourcesSpring = targetDataSources;
        
        Map<Object, Object> targetDataSourcesAll = new HashMap<Object, Object>();
        if(targetDataSourcesSpring != null){
        	targetDataSourcesAll.putAll(targetDataSourcesSpring);
        }
        //覆盖相同spring数据源
        if(targetDataSourcesPlugin != null){
        	for(Object obj : targetDataSourcesPlugin.keySet()){
        		if(targetDataSourcesAll.containsKey(obj)){
        			log.info("Spring 配置数据源[" + obj.toString() + "]被插件配置的同名数据源覆盖！");
        		}
        	}
        	targetDataSourcesAll.putAll(targetDataSourcesPlugin);
        }
        super.setTargetDataSources(targetDataSourcesAll);
        afterPropertiesSet();
    }
    
}