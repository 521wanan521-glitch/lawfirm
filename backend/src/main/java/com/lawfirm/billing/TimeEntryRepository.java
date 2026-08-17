package com.lawfirm.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long>, JpaSpecificationExecutor<TimeEntry> {

    long countByInvoiceId(Long invoiceId);

    List<TimeEntry> findByIdIn(List<Long> ids);

    long countByStatus(TimeEntryStatus status);

    @Query("select coalesce(sum(t.hours),0) from TimeEntry t where t.userId = :userId and t.workDate between :start and :end")
    BigDecimal sumHours(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select coalesce(sum(t.amount),0) from TimeEntry t where t.workDate between :start and :end and t.status <> 'SUBMITTED'")
    BigDecimal sumAmountBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 指定时间段内各律师工时排行 */
    @Query("select t.userId as userId, sum(t.hours) as hours from TimeEntry t " +
            "where t.workDate between :start and :end group by t.userId order by sum(t.hours) desc")
    List<Object[]> sumHoursGroupByUser(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
