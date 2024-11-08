package run.halo.dailyActive;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.dailyActive.request.InterfaceLogRequest;
import run.halo.dailyActive.vo.SelectorVO;
import run.halo.dailyActive.vo.InterfaceLogVO;

public interface InterfaceLogService {
    Flux<Boolean> deleteAll();

    Flux<SelectorVO> getAllUserInLog(String start);

    Flux<SelectorVO> getAllClientIPInLog(String start);

    Flux<SelectorVO> getAllRequestPathInLog(String start);

    Mono<ListResult<InterfaceLogVO>> getInterfaceLogByCondition(InterfaceLogRequest interfaceLogRequest);

    Mono<ListResult<InterfaceLogVO>> getAllInterfaceLog(InterfaceLogRequest interfaceLogRequest);
}
