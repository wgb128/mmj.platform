/*
 * Copyright 2011-2016 ZuoBian.com All right reserved. This software is the confidential and proprietary information of
 * ZuoBian.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with ZuoBian.com.
 */
package com.mmj.app.lucene.solr.service;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.TermsResponse.Term;
import org.springframework.beans.factory.annotation.Autowired;

import com.mmj.app.common.result.Result;
import com.mmj.app.lucene.search.cons.VersionType;
import com.mmj.app.lucene.solr.callback.SolrCallback;
import com.mmj.app.lucene.solr.client.SolrClient;
import com.mmj.app.lucene.solr.pojo.SearchField;
import com.mmj.app.lucene.solr.query.SearchQuery;

/**
 * @author zxc Sep 2, 2014 1:45:04 PM
 */
public abstract class BaseSearch<T extends SearchField, Q extends SearchQuery> implements SearchServiceConfig, VersionableSearch<T, Q> {

    @Autowired
    protected SolrClient solrClient;

    protected Class<T>   filedType;

    @SuppressWarnings("unchecked")
    public BaseSearch() {
        try {
            Type genericSuperclass = getClass().getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType) {
                ParameterizedType type = (ParameterizedType) genericSuperclass;
                Type[] actualTypeArguments = type.getActualTypeArguments();
                filedType = (Class<T>) actualTypeArguments[0];
            } else {
                throw new RuntimeException(String.format("没有找到【%s】的动态参数T", getClass().getSimpleName()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Integer getVersion(VersionType versionType) {
        if (versionType == null) {
            return null;
        }
        Integer maxVersion = 0;
        return maxVersion;
    }

    public void setSolrClient(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    protected Result beforeDel(Q query) {
        return null;
    }

    protected Result beforeSearch(Q query) {
        return null;
    }

    @Override
    public void indexWithOutDel(Integer version, List<T> fields) {
        logger.debug("indexWithOutDel: solrclient add bean feilds size:【{}】", fields.size());
        solrClient.addBeans(getCoreName(version), fields);
    }

    @Override
    public void indexWithOutDel(Integer version, T field) {
        solrClient.addBean(getCoreName(version), field);
    }

    @Override
    public Result delAll(Integer version) {
        return solrClient.delAll(getCoreName(version)) ? Result.success() : Result.failed();
    }

    @Override
    public Result del(Integer version, Q query) {
        Result result = beforeDel(query);
        if (result != null && !result.isSuccess()) {
            return result;
        }
        SolrQuery solrQuery = convert(query);
        return solrClient.del(getCoreName(version), solrQuery) ? Result.success() : Result.failed();
    }

    @Override
    public List<T> search(Integer version, final Q query, final SolrCallback callbacks) {
        Result result = beforeSearch(query);
        if (result != null && !result.isSuccess()) {
            return Collections.<T> emptyList();
        }
        SolrQuery solrQuery = convert(query);
        return solrClient.query(getCoreName(version), filedType, solrQuery, callbacks);
    }

    public List<Term> suggest(Integer version, Q query) {
        SolrQuery solrQuery = convert(query);
        return solrClient.suggest(getCoreName(version), solrQuery);
    }

    // ///////////////////////////////////////////////////
    // 抽象方法
    // ///////////////////////////////////////////////////

    public abstract String getCoreName(Integer version);

    public abstract SolrQuery convert(Q query);
}
