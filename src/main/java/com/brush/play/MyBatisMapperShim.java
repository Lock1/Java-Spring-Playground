package com.brush.play;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;



// Thin API over MyBatis's mapper providing at-runtime SQL statement generation & dynamic SQL processing interfaces
@Component
@AllArgsConstructor
@Slf4j
public class MyBatisMapperShim {
    private static final Set<String> JAVA_DEFAULT_OBJECT_METHOD_NAMES = Set.of("equals", "toString", "hashCode", "getClass", "notify", "notifyAll", "wait");

    private final MyBatisMapperAPI myBatisMapperApi;



    // All capital column names
    @FunctionalInterface
    public interface SqlResultHandler<Result> extends Function<Map<String,Object>,Result> {}

    public <Result> Stream<Result> executeSelect(
        String sqlStatement,
        SqlResultHandler<? extends Result> resultHandler
    ) {
        return myBatisMapperApi.select(Map.of(InternalSqlProvider.SQL_STATEMENT, sqlStatement))
            .stream()
            .map(resultHandler::apply);
    }

    public <Result,Parameter extends Record> Stream<Result> executeSelect(
        String sqlStatement,
        SqlResultHandler<? extends Result> resultHandler,
        Parameter param
    ) {
        final Map<String,Object> preparedParametersToBeInjected = new HashMap<>(); // Unavoidable hand written mutating for-loop: Collectors.toMap() rejects nulls
        for (final Method method: param.getClass().getMethods()) {
            if (!JAVA_DEFAULT_OBJECT_METHOD_NAMES.contains(method.getName())) {
                try {
                    preparedParametersToBeInjected.put(method.getName(), method.invoke(param));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException("Impossible java.lang.Record method invocation error", e);
                }
            }
        }
        final Map<String,Object> parameters = Map.ofEntries(
            Map.entry(InternalSqlProvider.SQL_STATEMENT, sqlStatement),
            Map.entry(InternalSqlProvider.DYNAMIC_PARAMETER_MUTATOR_NAME, (Consumer<Map<String,Object>>) preparedStatementMap -> preparedStatementMap.putAll(preparedParametersToBeInjected))
        );
        return myBatisMapperApi.select(parameters)
            .stream()
            .map(resultHandler::apply);
    }





    // This should never get exposed externally
    @Mapper
    interface MyBatisMapperAPI {
        // In conjunction with InternalSqlProvider, this provides poor-man: select(String sqlStatement, Consumer<Map<String,Object>>) -> List<Map<String,Object>>
        @SelectProvider(type=MyBatisMapperShim.InternalSqlProvider.class, method=InternalSqlProvider.MYBATIS_SQL_PROVIDER_METHOD_NAME)
        public List<Map<String,Object>> select(@Param(MyBatisMapperShim.InternalSqlProvider.__MYBATIS_PARAMETER) Map<String,Object> parameters);
    }

    // There's no point accessing this class, but it requires public visibility in order MyBatis @XXXProvider reflection to work
    public static final class InternalSqlProvider {
        private static final String __MYBATIS_PARAMETER              = "#__MYBATIS_PARAM"; // Should never clash with Record method names
        private static final String SQL_STATEMENT                    = "#__SQL_STATEMENT";
        private static final String DYNAMIC_PARAMETER_MUTATOR_NAME   = "#__DYNAMIC_PARAMETER_MUTATOR";
        private static final String MYBATIS_SQL_PROVIDER_METHOD_NAME = "sqlGenerator"; 

        @SuppressWarnings("unchecked") // This behavior based on reading MyBatis's source code. Last checked: MyBatis 3.5.19
        public String sqlGenerator(Map<String,Object> preparedStatementParameterMap) {
            final var parameters = (Map<String,Object>) preparedStatementParameterMap.get(__MYBATIS_PARAMETER);
            if (parameters.get(DYNAMIC_PARAMETER_MUTATOR_NAME) instanceof Consumer preparedStatementParameterMutator)
                 preparedStatementParameterMutator.accept(preparedStatementParameterMap);
            return (String) parameters.get(SQL_STATEMENT);
        }
    }
}
