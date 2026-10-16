package com.billsplit.web;

import com.billsplit.config.AppConfig;
import com.billsplit.dao.TransactionDao;
import com.billsplit.dao.TripDao;
import com.billsplit.dao.UserDao;
import com.billsplit.db.DataSourceFactory;
import com.billsplit.security.PasswordHasher;
import com.billsplit.service.AuthService;
import com.billsplit.service.ExpenseValidator;
import com.billsplit.service.SettlementCalculator;
import com.billsplit.service.SummaryService;
import com.billsplit.service.TransactionService;
import com.billsplit.service.TripService;
import jakarta.servlet.ServletContext;

public final class AppContext {
    public static final String ATTR = "billsplit.app";

    private final AppConfig config;
    private final DataSourceFactory dataSourceFactory;
    private final AuthService authService;
    private final TripService tripService;
    private final TransactionService transactionService;
    private final SettlementCalculator settlementCalculator;
    private final SummaryService summaryService;

    public AppContext(AppConfig config) {
        this.config = config;
        this.dataSourceFactory = new DataSourceFactory(config);
        UserDao userDao = new UserDao();
        TripDao tripDao = new TripDao();
        TransactionDao transactionDao = new TransactionDao();
        this.authService = new AuthService(dataSourceFactory, userDao, new PasswordHasher());
        this.tripService = new TripService(dataSourceFactory, tripDao, userDao);
        this.transactionService = new TransactionService(dataSourceFactory, tripDao, transactionDao, new ExpenseValidator());
        this.settlementCalculator = new SettlementCalculator();
        this.summaryService = new SummaryService(config);
    }

    public static AppContext from(ServletContext servletContext) {
        return (AppContext) servletContext.getAttribute(ATTR);
    }

    public AppConfig getConfig() {
        return config;
    }

    public DataSourceFactory getDataSourceFactory() {
        return dataSourceFactory;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public TripService getTripService() {
        return tripService;
    }

    public TransactionService getTransactionService() {
        return transactionService;
    }

    public SettlementCalculator getSettlementCalculator() {
        return settlementCalculator;
    }

    public SummaryService getSummaryService() {
        return summaryService;
    }
}
