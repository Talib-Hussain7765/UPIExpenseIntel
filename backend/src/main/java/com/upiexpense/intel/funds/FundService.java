package com.upiexpense.intel.funds;

import com.upiexpense.intel.common.error.BadRequestException;
import com.upiexpense.intel.common.error.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class FundService {

    private final FundSnapshotRepository fundSnapshotRepository;
    private final FundRepository fundRepository;
    private final FundTransactionRepository fundTransactionRepository;


    public FundService(FundSnapshotRepository fundSnapshotRepository, FundRepository fundRepository, FundTransactionRepository fundTransactionRepository) {
        this.fundSnapshotRepository = fundSnapshotRepository;
        this.fundRepository = fundRepository;
        this.fundTransactionRepository = fundTransactionRepository;
    }

    public FundResponse createFund(
            UUID userId,
            CreateFundRequest request
    ) {

        String name = request.name().trim();

        if (fundRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new BadRequestException("Fund already exists: " + name);
        }

//        Fund fund = Fund.builder()
//                .userId(userId)
//                .name(name)
//                .balance(BigDecimal.ZERO)
//                .build();

        Fund fund = new Fund();
        fund.setUserId(userId);
        fund.setName(name);

        log.info("Creating fund: {}",fund);

        fund = fundRepository.save(fund);

        return toResponse(fund);
    }

    @Transactional(readOnly = true)
    public List<FundResponse> getFunds(UUID userId) {

        return fundRepository
                .findByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FundSummaryResponse getFundSummary(UUID userId) {
        List<FundResponse> funds = getFunds(userId);
        BigDecimal allocatedBalance = funds.stream()
                .map(FundResponse::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FundSnapshot snapshot = fundSnapshotRepository
                .findFirstByUserIdOrderByReconciledDateDescCreatedAtDesc(userId)
                .orElse(null);
        BigDecimal totalBalance = snapshot == null ? allocatedBalance : snapshot.getTotalBalance();
        BigDecimal availableBalance = totalBalance.subtract(allocatedBalance);

        return new FundSummaryResponse(
                totalBalance,
                allocatedBalance,
                availableBalance,
                snapshot == null ? null : snapshot.getReconciledDate(),
                funds
        );
    }

    @Transactional(readOnly = true)
    public FundTransactionHistoryResponse getTransactionHistory(
            UUID userId,
            UUID fundId,
            FundActivityPeriod period,
            LocalDate startDate,
            LocalDate endDate
    ) {
        DateRange dateRange = resolveActivityRange(period, startDate, endDate);

        if (fundId != null) {
            fundRepository.findByIdAndUserId(fundId, userId)
                    .orElseThrow(() -> new NotFoundException("Fund not found"));
        }

        List<FundTransaction> transactions = fundId == null
                ? fundTransactionRepository
                        .findByUserIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                                userId,
                                dateRange.startDate(),
                                dateRange.endDate()
                        )
                : fundTransactionRepository
                        .findByUserIdAndFundIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                                userId,
                                fundId,
                                dateRange.startDate(),
                                dateRange.endDate()
                        );

        Map<UUID, String> fundNames = fundRepository
                .findByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .collect(Collectors.toMap(Fund::getId, Fund::getName));

        BigDecimal totalAdded = transactions.stream()
                .filter(transaction -> transaction.getType() == FundTransactionType.ADD)
                .map(FundTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWithdrawn = transactions.stream()
                .filter(transaction -> transaction.getType() == FundTransactionType.WITHDRAW)
                .map(FundTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<FundTransactionResponse> transactionResponses = transactions.stream()
                .map(transaction -> new FundTransactionResponse(
                        transaction.getId(),
                        transaction.getFundId(),
                        fundNames.getOrDefault(transaction.getFundId(), "Unknown fund"),
                        transaction.getAmount(),
                        transaction.getType(),
                        transaction.getTransactionDate(),
                        transaction.getNote(),
                        transaction.getCreatedAt()
                ))
                .toList();

        return new FundTransactionHistoryResponse(
                dateRange.startDate(),
                dateRange.endDate(),
                totalAdded,
                totalWithdrawn,
                totalAdded.subtract(totalWithdrawn),
                transactionResponses
        );
    }

    @Transactional
    public void reconcile(
            UUID userId,
            ReconcileFundRequest request
    ) {

        FundSnapshot snapshot = FundSnapshot.builder()
                .userId(userId)
                .totalBalance(request.totalBalance())
                .reconciledDate(request.reconciledDate())
                .build();

        fundSnapshotRepository.save(snapshot);
    }

    @Transactional
    public void updateFund(
            UUID userId,
            UUID fundId,
            UpdateFundRequest request
    ) {

        Fund fund = fundRepository.findByIdAndUserId(fundId, userId)
                .orElseThrow(() -> new NotFoundException("Fund not found"));

        BigDecimal amount = request.amount();

        if (request.type() == FundTransactionType.ADD) {

            fund.setBalance(
                    fund.getBalance().add(amount)
            );

        } else {

            if (fund.getBalance().compareTo(amount) < 0) {
                throw new BadRequestException("Insufficient fund balance");
            }

            fund.setBalance(
                    fund.getBalance().subtract(amount)
            );
        }

        FundTransaction transaction = FundTransaction.builder()
                .userId(userId)
                .fundId(fundId)
                .amount(amount)
                .type(request.type())
                .transactionDate(request.transactionDate())
                .note(request.note())
                .build();

        fundRepository.save(fund);
        fundTransactionRepository.save(transaction);
    }

    private FundResponse toResponse(Fund fund) {

        return new FundResponse(
                fund.getId(),
                fund.getName(),
                fund.getBalance()
        );
    }

    private DateRange resolveActivityRange(
            FundActivityPeriod period,
            LocalDate startDate,
            LocalDate endDate
    ) {
        FundActivityPeriod effectivePeriod = period == null
                ? FundActivityPeriod.LAST_MONTH
                : period;

        if (effectivePeriod == FundActivityPeriod.LAST_MONTH) {
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            return new DateRange(lastMonth.atDay(1), lastMonth.atEndOfMonth());
        }

        if (effectivePeriod == FundActivityPeriod.LAST_3_MONTHS) {
            LocalDate today = LocalDate.now();
            return new DateRange(today.minusMonths(3).plusDays(1), today);
        }

        if (startDate == null || endDate == null) {
            throw new BadRequestException("Start date and end date are required for a custom period");
        }

        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("End date must be on or after start date");
        }

        return new DateRange(startDate, endDate);
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
