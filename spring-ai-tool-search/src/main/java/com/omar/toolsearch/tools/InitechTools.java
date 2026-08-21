package com.omar.toolsearch.tools;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class InitechTools {

	@Tool(name = "platform_getCurrentDateTime",
			description = "Get the current date and time. Always call this before any date reasoning so you never assume today's date.")
	public String getCurrentDateTime() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd HH:mm"));
	}

	@Tool(name = "hr_lookupEmployee",
			description = "Look up a single employee's HR profile by employee ID, returning their name, title, department, home office, and manager.")
	public String lookupEmployee(@ToolParam(description = "Employee ID, e.g. E-4471") String employeeId) {
		if ("E-4471".equalsIgnoreCase(employeeId)) {
			return """
					Employee %s
					- Name: Jane Doe
					- Title: Senior Solutions Architect
					- Department: Cloud Platform
					- Home office: Austin HQ (AUS)
					- Manager: Michael Chen (E-1002)
					- Email: jane.doe@initech.example""".formatted(employeeId);
		}
		return "Employee %s: record found (demo placeholder — no detailed profile on file).".formatted(employeeId);
	}

	@Tool(name = "travel_searchFlights",
			description = "Search available flights between two cities on a given date, returning flight numbers, times, and prices.")
	public List<String> searchFlights(@ToolParam(description = "Origin city or airport") String fromCity,
			@ToolParam(description = "Destination city or airport") String toCity,
			@ToolParam(description = "Departure date, YYYY-MM-DD") String date) {
		return List.of(
				"IN417  %s -> %s  %s  dep 08:15  arr 23:40  (1 stop)  $842".formatted(fromCity, toCity, date),
				"IN612  %s -> %s  %s  dep 11:50  arr 06:20+1 (1 stop) $769".formatted(fromCity, toCity, date),
				"IN905  %s -> %s  %s  dep 16:05  arr 09:55+1 (1 stop) $913".formatted(fromCity, toCity, date));
	}

	@Tool(name = "travel_bookFlight",
			description = "Book a specific flight for an employee by flight number and date, returning a booking confirmation code.")
	public String bookFlight(@ToolParam(description = "Flight number, e.g. IN417") String flightNumber,
			@ToolParam(description = "Employee ID, e.g. E-4471") String employeeId,
			@ToolParam(description = "Departure date, YYYY-MM-DD") String date) {
		return "Booked flight %s for employee %s on %s. Confirmation: IN-7Q4KZP.".formatted(flightNumber, employeeId,
				date);
	}

	@Tool(name = "facilities_reserveDesk",
			description = "Reserve a hot desk for an employee at a given office for a number of days, returning the desk assignment.")
	public String reserveDesk(@ToolParam(description = "Office location, e.g. Berlin") String office,
			@ToolParam(description = "Start date, YYYY-MM-DD") String startDate,
			@ToolParam(description = "Number of days to reserve") int days,
			@ToolParam(description = "Employee ID, e.g. E-4471") String employeeId) {
		return "Reserved desk B-14 (floor 3) at the %s office for employee %s, starting %s for %d day(s)."
			.formatted(office, employeeId, startDate, days);
	}

	@Tool(name = "weather_getForecast",
			description = "Get the weather forecast for a city on a specific date so a traveler knows what to pack.")
	public String getForecast(@ToolParam(description = "City, e.g. Berlin") String city,
			@ToolParam(description = "Date, YYYY-MM-DD") String date) {
		return "%s on %s: partly cloudy, high 14C / low 6C, light rain in the afternoon. Pack a warm layer and a compact umbrella."
			.formatted(city, date);
	}

	// Filler catalog: the searchable signal lives entirely in each name + description.

	// ---- HR ----
	@Tool(description = "List all employees in a given department.")
	public String hr_listEmployees(@ToolParam(description = "Department name") String department) {
		return stub();
	}

	@Tool(description = "Get an employee's remaining paid-time-off (PTO) balance in days.")
	public String hr_ptoBalance(@ToolParam(description = "Employee ID") String employeeId) {
		return stub();
	}

	@Tool(description = "Submit a paid-time-off request for an employee for a date range.")
	public String hr_submitTimeOff(@ToolParam(description = "Employee ID") String employeeId) {
		return stub();
	}

	@Tool(description = "Return the reporting chain (org chart) above an employee.")
	public String hr_orgChart(@ToolParam(description = "Employee ID") String employeeId) {
		return stub();
	}

	// ---- Finance ----
	@Tool(description = "File an expense report for an employee and return a reimbursement reference.")
	public String finance_fileExpenseReport(@ToolParam(description = "Employee ID") String employeeId) {
		return stub();
	}

	@Tool(description = "Approve a pending vendor invoice by invoice ID.")
	public String finance_approveInvoice(@ToolParam(description = "Invoice ID") String invoiceId) {
		return stub();
	}

	@Tool(description = "Get the current budget status and remaining spend for a department.")
	public String finance_budgetStatus(@ToolParam(description = "Department name") String department) {
		return stub();
	}

	@Tool(description = "Convert an amount between currencies at today's exchange rate.")
	public String finance_convertCurrency(@ToolParam(description = "Amount and currencies, e.g. '100 USD to EUR'") String request) {
		return stub();
	}

	// ---- DevOps ----
	@Tool(description = "Deploy a named service to an environment and return the release id.")
	public String devops_deployService(@ToolParam(description = "Service name") String service) {
		return stub();
	}

	@Tool(description = "Roll a named service back to its previous release.")
	public String devops_rollbackService(@ToolParam(description = "Service name") String service) {
		return stub();
	}

	@Tool(description = "List the CI/CD pipelines defined for a project.")
	public String devops_listPipelines(@ToolParam(description = "Project key") String project) {
		return stub();
	}

	@Tool(description = "Scale a service to a target number of running instances.")
	public String devops_scaleService(@ToolParam(description = "Service name") String service) {
		return stub();
	}

	// ---- CRM ----
	@Tool(description = "Find a customer account by company name.")
	public String crm_findAccount(@ToolParam(description = "Company name") String name) {
		return stub();
	}

	@Tool(description = "Create a new sales lead from a contact email address.")
	public String crm_createLead(@ToolParam(description = "Contact email") String email) {
		return stub();
	}

	@Tool(description = "Log a phone call against a customer account.")
	public String crm_logCall(@ToolParam(description = "Account ID") String accountId) {
		return stub();
	}

	@Tool(description = "Return the sales pipeline forecast for a region.")
	public String crm_pipelineForecast(@ToolParam(description = "Region name") String region) {
		return stub();
	}

	// ---- Support ----
	@Tool(description = "Open a new customer support ticket with a subject line.")
	public String support_openTicket(@ToolParam(description = "Ticket subject") String subject) {
		return stub();
	}

	@Tool(description = "Get the current status of a support ticket by id.")
	public String support_ticketStatus(@ToolParam(description = "Ticket ID") String ticketId) {
		return stub();
	}

	@Tool(description = "Escalate a support ticket to the next support tier.")
	public String support_escalateTicket(@ToolParam(description = "Ticket ID") String ticketId) {
		return stub();
	}

	@Tool(description = "Search the support knowledge base for help articles matching a query.")
	public String support_searchKnowledgeBase(@ToolParam(description = "Search query") String query) {
		return stub();
	}

	// ---- IT ----
	@Tool(description = "Reset an employee's account password and send a temporary one.")
	public String it_resetPassword(@ToolParam(description = "Username") String username) {
		return stub();
	}

	@Tool(description = "Provision a new laptop for an employee and return an asset tag.")
	public String it_provisionLaptop(@ToolParam(description = "Employee ID") String employeeId) {
		return stub();
	}

	@Tool(description = "Grant an employee access to an internal system or application.")
	public String it_grantAccess(@ToolParam(description = "System or application name") String system) {
		return stub();
	}

	@Tool(description = "Check whether an employee's VPN account is active.")
	public String it_checkVpnStatus(@ToolParam(description = "Username") String username) {
		return stub();
	}

	// ---- Marketing ----
	@Tool(description = "Schedule a marketing campaign to launch on a date.")
	public String marketing_scheduleCampaign(@ToolParam(description = "Campaign name") String name) {
		return stub();
	}

	@Tool(description = "Send an email blast to a named audience segment.")
	public String marketing_sendEmailBlast(@ToolParam(description = "Audience segment") String segment) {
		return stub();
	}

	@Tool(description = "Schedule a social media post to a channel.")
	public String marketing_schedulePost(@ToolParam(description = "Channel, e.g. LinkedIn") String channel) {
		return stub();
	}

	// ---- Analytics ----
	@Tool(description = "Run a saved analytics report and return a link to the results.")
	public String analytics_runReport(@ToolParam(description = "Report name") String name) {
		return stub();
	}

	@Tool(description = "Get the dashboard URL for a team's key metrics.")
	public String analytics_dashboardUrl(@ToolParam(description = "Team name") String team) {
		return stub();
	}

	@Tool(description = "Return the latest snapshot value for a named KPI.")
	public String analytics_kpiSnapshot(@ToolParam(description = "Metric name") String metric) {
		return stub();
	}

	// ---- Security ----
	@Tool(description = "Run a security vulnerability scan against a source repository.")
	public String security_scanRepository(@ToolParam(description = "Repository name") String repo) {
		return stub();
	}

	@Tool(description = "Rotate a named secret or API key and invalidate the old value.")
	public String security_rotateSecret(@ToolParam(description = "Secret name") String name) {
		return stub();
	}

	@Tool(description = "Review the security audit log for a user's recent activity.")
	public String security_reviewAuditLog(@ToolParam(description = "Username") String user) {
		return stub();
	}

	// ---- Facilities ----
	@Tool(description = "Book a meeting room at an office for a time slot.")
	public String facilities_bookRoom(@ToolParam(description = "Office location") String office) {
		return stub();
	}

	@Tool(description = "Report a facilities issue (e.g. broken AC) at an office.")
	public String facilities_reportIssue(@ToolParam(description = "Office location") String office) {
		return stub();
	}

	@Tool(description = "Find an available parking spot at an office.")
	public String facilities_findParking(@ToolParam(description = "Office location") String office) {
		return stub();
	}

	// ---- Travel ----
	@Tool(description = "Book a hotel room in a city for a stay.")
	public String travel_bookHotel(@ToolParam(description = "City") String city) {
		return stub();
	}

	@Tool(description = "Reserve a rental car in a city for a date range.")
	public String travel_rentCar(@ToolParam(description = "City") String city) {
		return stub();
	}

	@Tool(description = "Get the full travel itinerary already booked for an employee.")
	public String travel_getItinerary(@ToolParam(description = "Employee ID") String employeeId) {
		return stub();
	}

	// ---- Platform ----
	@Tool(description = "Check whether a feature flag is enabled.")
	public String platform_getFeatureFlag(@ToolParam(description = "Feature flag name") String flag) {
		return stub();
	}

	@Tool(description = "Get the health status of an internal platform service.")
	public String platform_getServiceHealth(@ToolParam(description = "Service name") String service) {
		return stub();
	}

	private static String stub() {
		return "ok (demo stub — Initech tools don't call a real backend).";
	}

}