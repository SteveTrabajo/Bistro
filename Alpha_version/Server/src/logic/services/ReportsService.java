package logic.services;

import java.time.LocalDate;

import entities.MonthlyReport;
import logic.BistroDataBase_Controller;
import logic.ServerLogger;

public class ReportsService {
	private final BistroDataBase_Controller dbController;
	private final ServerLogger logger;
	public ReportsService(BistroDataBase_Controller dbController, ServerLogger logger) {
		this.dbController = dbController;
		this.logger = logger;
	}
	
	public MonthlyReport getMontlyReport(LocalDate date) {
		MonthlyReport monthlyReport = new MonthlyReport();
		
		monthlyReport.setTotalReservations(this.dbController.getTotalReservation(date));
		monthlyReport.setTotalCostumer(this.dbController.getTotalCostumersInMonth(date));
		monthlyReport.setTotalLateCostumer(this.dbController.getTotalLateCostumersInMonth(date));
		monthlyReport.setTotalOnTimeCostumer(this.dbController.getTotalOntTimeCostumersInMonth(date));
		monthlyReport.setTotalMemberReservations(this.dbController.getTotalMembersReservationInMonth(date));
		
		int totalReservation = monthlyReport.getTotalReservations();
		int totalMemberReservation = monthlyReport.getTotalMemberReservations();
		if(totalReservation == 0) {
			monthlyReport.setMemberReservationPrecetage(0);
		}
		else {
			double mebmerRateInReservations = (int)Math.round((totalMemberReservation * 100.0) / totalReservation);
			monthlyReport.setMemberReservationPrecetage(mebmerRateInReservations);
		}
		
		
		return monthlyReport;
	}
	
}
