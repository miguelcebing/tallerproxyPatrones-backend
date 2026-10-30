package com.miguelcebing.tallerproxy.logging;

import com.miguelcebing.tallerproxy.domain.LogEntry;
import com.miguelcebing.tallerproxy.domain.LogQuery;
import com.miguelcebing.tallerproxy.domain.PageResult;
import java.util.List;

public interface LogRepository {

    void save(LogEntry entry);

    List<LogEntry> findAll();

    PageResult<LogEntry> find(LogQuery query);

    long count();

    void clear();
}
