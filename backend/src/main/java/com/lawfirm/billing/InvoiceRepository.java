package com.lawfirm.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    long countByInvoiceNoStartingWith(String prefix);

    /** 指定时间段内已开票+已收款的账单总额 */
    @Query("select coalesce(sum(i.totalAmount),0) from Invoice i " +
            "where i.status in ('ISSUED','PAID') and i.issueDate between :start and :end")
    BigDecimal sumAmountByIssueDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select i.status as status, count(i) as cnt from Invoice i group by i.status")
    List<Object[]> countGroupByStatus();
}
