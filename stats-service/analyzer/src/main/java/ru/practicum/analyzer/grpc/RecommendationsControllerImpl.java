package ru.practicum.analyzer.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.proto.*;

@GrpcService
@RequiredArgsConstructor
public class RecommendationsControllerImpl extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        recommendationService.getRecommendationsForUser(request.getUserId(), request.getMaxResults())
                .forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        recommendationService.getSimilarEvents(request.getEventId(), request.getUserId(), request.getMaxResults())
                .forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        recommendationService.getInteractionsCount(request.getEventIdList())
                .forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }
}