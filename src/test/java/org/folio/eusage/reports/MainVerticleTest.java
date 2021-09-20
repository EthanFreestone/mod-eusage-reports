package org.folio.eusage.reports;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.tlib.postgres.TenantPgPoolContainer;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.startsWith;

@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {
  private final static Logger log = LogManager.getLogger("MainVerticleTest");

  static Vertx vertx;
  static final int MODULE_PORT = 9230;
  static final int MOCK_PORT = 9231;
  static final String POLINE_NUMBER_SAMPLE = "121x-219";
  static final String pubDateSample = "1998-05-01";
  static final String pubYearSample = "1999";
  static final UUID goodKbTitleId = UUID.randomUUID();
  static boolean enableGoodKbTitle;
  static final String goodKbTitleISSN = "1000-1002";
  static final String goodKbTitleISSNstrip = "10001002";
  static final String goodDoiValue = "publisherA:Code123";
  static final UUID otherKbTitleId = UUID.randomUUID();
  static final String otherKbTitleISSN = "1000-2000";
  static final String noMatchKbTitleISSN = "1001-1002";
  static final String noMatchKbTitleISSNstrip = "10011002";
  static final UUID goodCounterReportId = UUID.randomUUID();
  static final UUID otherCounterReportId = UUID.randomUUID();
  static final UUID badJsonCounterReportId = UUID.randomUUID();
  static final UUID badStatusCounterReportId = UUID.randomUUID();
  static final UUID goodAgreementId = UUID.randomUUID();
  static final UUID badJsonAgreementId = UUID.randomUUID();
  static final UUID badStatusAgreementId = UUID.randomUUID();
  static final UUID badStatusAgreementId2 = UUID.randomUUID();
  static final UUID usageProviderId = UUID.randomUUID();
  static final UUID goodFundId = UUID.randomUUID();
  static final UUID goodLedgerId = UUID.randomUUID();
  static final UUID goodFiscalYearId = UUID.randomUUID();
  static final UUID[] agreementLineIds = {
      UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
  };
  static final UUID[] poLineIds = {
      UUID.randomUUID(), UUID.randomUUID()
  };
  static final UUID goodPackageId = UUID.randomUUID();
  static final UUID[] packageTitles = {
      UUID.randomUUID(), UUID.randomUUID()
  };

  @ClassRule
  public static PostgreSQLContainer<?> postgresSQLContainer = TenantPgPoolContainer.create();

  static JsonObject getCounterReportMock(UUID id, int cnt) {
    JsonObject counterReport = new JsonObject();
    counterReport.put("id", id);
    counterReport.put("providerId", usageProviderId);
    counterReport.put("yearMonth", "2021-01");
    JsonObject report = new JsonObject();
    counterReport.put("report", report);
    report.put("vendor", new JsonObject()
        .put("id", "This is take vendor")
        .put("contact", new JsonArray())
    );
    report.put("providerId", "not_inspected");
    report.put("yearMonth", "not_inspected");
    report.put("name", "JR1");
    report.put("title", "Journal Report " + cnt);
    report.put("customer", new JsonArray()
        .add(new JsonObject()
            .put("id", "fake customer id")
            .put(cnt > 1 ? "reportItems" : "Report_Items", new JsonArray()
                .add(new JsonObject()
                    .put("itemName", "The cats journal")
                    .put("itemDataType", "JOURNAL")
                    .put("itemIdentifier", new JsonArray()
                        .add(new JsonObject()
                            .put("type", "DOI")
                            .put("value", goodDoiValue)
                        )
                        .add(new JsonObject()
                            .put("type", "PRINT_ISSN")
                            .put("value", goodKbTitleISSN)
                        )
                        .add(new JsonObject()
                            .put("type", "Publication_Date")
                            .put("value", pubDateSample)
                        )
                    )
                    .put("itemPerformance", new JsonArray()
                        .add(new JsonObject()
                            .put("Period", new JsonObject()
                                .put("End_Date", "2021-01-31")
                                .put("Begin_Date", "2021-01-01")
                            )
                            .put("category", "REQUESTS")
                            .put("instance", new JsonArray()
                                .add(new JsonObject()
                                    .put("count", 5)
                                    .put("Metric_Type", "Total_Item_Requests")
                                )
                                .add(new JsonObject()
                                    .put("count", 3)
                                    .put("Metric_Type", "Unique_Item_Requests")
                                )
                            )
                        )
                    )
                )
                .add(new JsonObject()
                    .put("YOP", pubYearSample)
                    .put("Title", cnt == -1 ? "The other journal" : "The dogs journal")
                    .put("itemDataType", "JOURNAL")
                    .put("Item_ID", new JsonArray()
                        .add(new JsonObject()
                            .put("type", "Print_ISSN")
                            .put("value", "1001-1001")
                        )
                        .add(null)
                        .add(new JsonObject()
                            .put("type", "Online_ISSN")
                            .put("value",  cnt == -1 ? otherKbTitleISSN : noMatchKbTitleISSN)
                        )
                    )
                    .put("Performance", new JsonArray()
                        .add(null)
                        .add(new JsonObject()
                            .put("Period", new JsonObject()
                                .put("End_Date", "2021-01-31")
                                .put("Begin_Date", "2021-01-01")
                            )
                            .put("category", "REQUESTS")
                            .put("instance", new JsonArray()
                                .add(new JsonObject()
                                    .put("Count", 35)
                                    .put("Metric_Type", "Total_Item_Requests")
                                )
                                .add(null)
                                .add(new JsonObject()
                                    .put("Count", 30)
                                    .put("Metric_Type", "Unique_Item_Requests")
                                )
                            )
                        )
                    )
                )
                .add(new JsonObject()
                    .put("itemName", "Best " + cnt + " pets of all time")
                    .put("itemDataType", "JOURNAL")
                    .put("itemIdentifier", new JsonArray()
                        .add(new JsonObject()
                            .put("type", "ISBN")
                            .put("value", "978-3-16-148410-" + String.format("%d", cnt))
                        )
                    )
                    .put("itemPerformance", new JsonArray()
                        .add(new JsonObject()
                            .put("Period", new JsonObject()
                                .put("End_Date", "2021-01-31")
                                .put("Begin_Date", "2021-01-01")
                            )
                            .put("category", "REQUESTS")
                            .put("instance", new JsonArray()
                                .add(new JsonObject()
                                    .put("count", 135)
                                    .put("Metric_Type", "Total_Item_Requests")
                                )
                                .add(new JsonObject()
                                    .put("count", 120)
                                    .put("Metric_Type", "Unique_Item_Requests")
                                )
                            )
                        )
                    )
                )
                .add(new JsonObject()
                    .put("itemName", "No match")
                    .put("itemDataType", "JOURNAL")
                    .put("itemIdentifier", new JsonArray()
                        .add(new JsonObject()
                            .put("type", "Proprietary_ID")
                            .put("value", "10.10ZZ")
                        )
                    )
                    .put("itemPerformance", new JsonArray())
                )
                .add(new JsonObject()
                    .put("Platform", "My Platform")
                    .put("itemPerformance", new JsonArray())
                )
            )
        )
    );
    return counterReport;
  }

  static void getCounterReportsChunk(RoutingContext ctx, int offset, int limit, int max, boolean first) {
    if (offset >= max || limit <= 0) {
      ctx.response().end("], \"totalRecords\": " + max + "}");
      return;
    }
    String lead = first ? "" : ",";
    JsonObject counterReport = getCounterReportMock(UUID.randomUUID(), offset + 1);
    ctx.response().write(lead + counterReport.encode())
        .onComplete(x -> getCounterReportsChunk(ctx, offset + 1, limit - 1, max, false));
  }

  static void getCounterReports(RoutingContext ctx) {
    ctx.response().setChunked(true);
    ctx.response().putHeader("Content-Type", "application/json");
    ctx.response().write("{ \"counterReports\": [ ")
        .onComplete(x -> {
          String limit = ctx.request().getParam("limit");
          String offset = ctx.request().getParam("offset");
          int total = 5;
          String query = ctx.request().getParam("query");
          if (query != null) {
            UUID matchProviderId = UUID.fromString(query.substring(query.lastIndexOf('=') + 1));
            if (!matchProviderId.equals(usageProviderId)) {
              total = 0;
            }
          }
          getCounterReportsChunk(ctx, offset == null ? 0 : Integer.parseInt(offset),
              limit == null ? 10 : Integer.parseInt(limit), total, true);
        });
  }

  static void getCounterReport(RoutingContext ctx) {
    String path = ctx.request().path();
    int offset = path.lastIndexOf('/');
    UUID id = UUID.fromString(path.substring(offset + 1));
    if (id.equals(goodCounterReportId)) {
      ctx.response().setChunked(true);
      ctx.response().putHeader("Content-Type", "application/json");
      ctx.response().end(getCounterReportMock(id, 0).encode());
    } else if (id.equals(otherCounterReportId)) {
        ctx.response().setChunked(true);
        ctx.response().putHeader("Content-Type", "application/json");
        ctx.response().end(getCounterReportMock(id, -1).encode());
    } else  if (id.equals(badStatusCounterReportId)) {
      ctx.response().putHeader("Content-Type", "text/plain");
      ctx.response().setStatusCode(403);
      ctx.response().end("forbidden");
    } else  if (id.equals(badJsonCounterReportId)) {
      ctx.response().setChunked(true);
      ctx.response().putHeader("Content-Type", "application/json");
      ctx.response().end("{");
    } else {
      ctx.response().putHeader("Content-Type", "text/plain");
      ctx.response().setStatusCode(404);
      ctx.response().end("not found");
    }
  }

  static JsonObject getKbTitle(UUID kbTitleId) {
    JsonObject res = new JsonObject();
    if (goodKbTitleId.equals(kbTitleId)) {
      res.put("name", "good kb title instance name");
      res.put("id", kbTitleId);
      res.put("publicationType", new JsonObject().put("value", "monograph"));
      res.put("identifiers", new JsonArray()
          .add(new JsonObject()
              .put("identifier", new JsonObject()
                  .put("value", goodKbTitleISSN)
              )
          ));
    } else if (otherKbTitleId.equals(kbTitleId)) {
      res.put("name", "other kb title instance name");
      res.put("id", kbTitleId);
      res.put("identifiers", new JsonArray()
          .add(new JsonObject()
              .put("identifier", new JsonObject()
                  .put("value", otherKbTitleISSN)
              )
          ));
    } else {
      res.put("name", "fake kb title instance name");
      res.put("publicationType", new JsonObject().put("value", "serial"));
      res.put("id", kbTitleId);
      res.put("identifiers", new JsonArray()
          .add(new JsonObject()
              .put("identifier", new JsonObject()
                  .put("value", "1000-9999")
              )
          ));
    }
    return res;
  }
  static void getErmResource(RoutingContext ctx) {
    ctx.response().setChunked(true);
    ctx.response().putHeader("Content-Type", "application/json");
    String term = ctx.request().getParam("term");
    JsonArray ar = new JsonArray();
    UUID kbTitleId;
    switch (term) {
      case goodKbTitleISSN:
      case goodKbTitleISSNstrip:
        kbTitleId = enableGoodKbTitle ? goodKbTitleId : null;
        break; // return a known kbTitleId for "The cats journal"
      case otherKbTitleISSN:
        kbTitleId = otherKbTitleId;
        break;
      case noMatchKbTitleISSN:
      case noMatchKbTitleISSNstrip:
        kbTitleId = null; // for "The dogs journal" , no kb match
        break;
      default:
        kbTitleId = UUID.randomUUID();
    }
    if (kbTitleId != null) {
      ar.add(getKbTitle(kbTitleId));
    }
    ctx.response().end(ar.encode());
  }

  static void getErmResourceId(RoutingContext ctx) {
    String path = ctx.request().path();
    int offset = path.lastIndexOf('/');
    UUID id = UUID.fromString(path.substring(offset + 1));

    ctx.response().setChunked(true);
    ctx.response().putHeader("Content-Type", "application/json");
    ctx.response().end(getKbTitle(id).encode());
  }

  static void getErmResourceEntitlement(RoutingContext ctx) {
    ctx.response().setChunked(true);
    ctx.response().putHeader("Content-Type", "application/json");
    String term = ctx.request().getParam("term");
    JsonArray ar = new JsonArray();
    if ("org.olf.kb.Pkg".equals(term)) {
      ar.add(new JsonObject()
          .put("id", UUID.randomUUID())
          .put("name", "fake kb package name")
      );
    }
    ctx.response().end(ar.encode());
  }

  static void getAgreement(RoutingContext ctx) {
    String path = ctx.request().path();
    int offset = path.lastIndexOf('/');
    UUID id = UUID.fromString(path.substring(offset + 1));
    if (id.equals(goodAgreementId) || id.equals(badStatusAgreementId2)) {
      ctx.response().setChunked(true);
      ctx.response().putHeader("Content-Type", "application/json");
      ctx.response().end(new JsonObject().encode());
    } else if (id.equals(badStatusAgreementId)) {
      ctx.response().putHeader("Content-Type", "text/plain");
      ctx.response().setStatusCode(403);
      ctx.response().end("forbidden");
    } else  if (id.equals(badJsonAgreementId)) {
      ctx.response().setChunked(true);
      ctx.response().putHeader("Content-Type", "application/json");
      ctx.response().end("{");
    } else {
      ctx.response().putHeader("Content-Type", "text/plain");
      ctx.response().setStatusCode(404);
      ctx.response().end("not found");
    }
  }

  static void getEntitlements(RoutingContext ctx) {
    String filters = ctx.request().getParam("filters");
    if (filters == null) {
      ctx.response().putHeader("Content-Type", "text/plain");
      ctx.response().setStatusCode(400);
      ctx.response().end("filters missing");
      return;
    }
    UUID agreementId = badStatusAgreementId;
    if (filters.startsWith("owner=")) {
      agreementId = UUID.fromString(filters.substring(6));
    }
    if (agreementId.equals(badJsonAgreementId)) {
      ctx.response().setChunked(true);
      ctx.response().putHeader("Content-Type", "application/json");
      ctx.response().end("[{]");
      return;
    }
    if (agreementId.equals(badStatusAgreementId2)) {
      ctx.response().putHeader("Content-Type", "text/plain");
      ctx.response().setStatusCode(500);
      ctx.response().end("internal error");
      return;
    }
    JsonArray ar = new JsonArray();
    if (agreementId.equals(goodAgreementId)) {
      for (int i = 0; i < agreementLineIds.length; i++) {
        JsonArray poLinesAr = new JsonArray();
        for (int j = 0; j < i && j < poLineIds.length; j++) {
          poLinesAr.add(new JsonObject()
              .put("poLineId", poLineIds[j])
          );
        }
        if (i == 0) {
          // fake package
          ar.add(new JsonObject()
              .put("id", agreementLineIds[i])
              .put("owner", new JsonObject()
                  .put("id", goodAgreementId)
                  .put("name", "Good agreement"))
              .put("resource", new JsonObject()
                  .put("class", "org.olf.kb.Pkg")
                  .put("name", "good package name")
                  .put("id", goodPackageId)
                  .put("_object", new JsonObject()
                  ))
              .put("poLines", poLinesAr)
          );
        } else {
          // fake package content item
          JsonArray coverage = new JsonArray();
          UUID kbtitleId;
          switch (i) {
            case 1:
              coverage.add(new JsonObject()
                  .put("startDate", "2020-03-09")
                  .put("endDate", "2020-04-05")
              );
              kbtitleId = goodKbTitleId;
              break;
            case 2:
              coverage.add(new JsonObject()
                  .put("startDate", "2021-03-09")
              );
              kbtitleId = otherKbTitleId;
              break;
            default:
              kbtitleId = UUID.randomUUID();
          }
          ar.add(new JsonObject()
              .put("id", agreementLineIds[i])
              .put("owner", new JsonObject()
                  .put("id", goodAgreementId)
                  .put("name", "Good agreement"))
              .put("resource", new JsonObject()
                  .put("class", "org.olf.kb.PackageContentItem")
                  .put("id", UUID.randomUUID())
                  .put("coverage", coverage)
                  .put("_object", new JsonObject()
                      .put("pti", new JsonObject()
                          .put("titleInstance", new JsonObject()
                              .put("id", kbtitleId)
                              .put("publicationType", new JsonObject()
                                  .put("value", "serial")
                              )
                          )
                      )
                  )
              )
              .put("poLines", poLinesAr)
          );
        }
      }
    }
    ctx.response().setChunked(true);
    ctx.response().putHeader("Content-Type", "application/json");
    ctx.response().end(ar.encode());
  }

  static List<String> orderLinesCurrencies = new LinkedList<>();

  static void getOrderLines(RoutingContext ctx) {
    String path = ctx.request().path();
    int offset = path.lastIndexOf('/');
    UUID id = UUID.fromString(path.substring(offset + 1));
    for (int i = 0; i < poLineIds.length; i++) {
      if (id.equals(poLineIds[i])) {
        ctx.response().setChunked(true);
        ctx.response().putHeader("Content-Type", "application/json");
        JsonObject orderLine = new JsonObject();
        orderLine.put("id", id);
        orderLine.put("poLineNumber", POLINE_NUMBER_SAMPLE);
        String currency = i < orderLinesCurrencies.size() ? orderLinesCurrencies.get(i) : "USD";
        orderLine.put("cost", new JsonObject()
            .put("currency", currency)
            .put("listUnitPriceElectronic", 100.0 + (i * i))
        );
        if (i == 0) {
          orderLine.put("fundDistribution", new JsonArray()
              .add(new JsonObject()
                  .put("fundId", goodFundId.toString())
              )
          );
          orderLine.put("purchaseOrderId", UUID.randomUUID().toString());
        }
        ctx.response().end(orderLine.encode());
        return;
      }
    }
    ctx.response().putHeader("Content-Type", "text/plain");
    ctx.response().setStatusCode(404);
    ctx.response().end("Order line not found");
  }

  static void getInvoiceLines(RoutingContext ctx) {
    String query = ctx.request().getParam("query");
    if (query == null || !query.startsWith("poLineId==")) {
      ctx.response().putHeader("Content-Type", "text/plain");
      ctx.response().setStatusCode(400);
      ctx.response().end("query missing");
      return;
    }
    String limit = ctx.request().getParam("limit");
    if (!"2147483647".equals(limit)) {
      ctx.response().putHeader("Content-Type", "text/plain");
      ctx.response().setStatusCode(400);
      ctx.response().end("limit missing");
      return;
    }
    UUID poLineId = UUID.fromString(query.substring(10));

    JsonArray ar = new JsonArray();
    for (int i = 0; i < poLineIds.length; i++) {
      if (poLineId.equals(poLineIds[i])) {
        {
          JsonObject invoice = new JsonObject()
              .put("poLineId", poLineId)
              .put("quantity", 1 + i)
              .put("subTotal", 10.0 + i * 5)
              .put("total", 12.0 + i * 6)
              .put("invoiceLineNumber", String.format("%d", i));
          if (i == 0) {
            invoice.put("subscriptionStart", "2020-01-01T00:00:00.000+00:00");
            invoice.put("subscriptionEnd", "2020-12-31T00:00:00.000+00:00");
          }
          ar.add(invoice);
        }
      }
    }
    ar.add(new JsonObject()
        .put("poLineId", poLineId)
    );
    ctx.response().setChunked(true);
    ctx.response().putHeader("Content-Type", "application/json");
    ctx.response().end(new JsonObject().put("invoiceLines", ar).encode());
  }

  static void getPackageContent(RoutingContext ctx) {
    String path = ctx.request().path();
    UUID id = UUID.fromString(path.substring(14, 50));
    if (!id.equals(goodPackageId)) {
      ctx.response().putHeader("Content-Type", "text/plain");
      ctx.response().setStatusCode(404);
      ctx.response().end("Package not found");
      return;
    }
    JsonArray ar = new JsonArray();
    for (UUID packageTitle : packageTitles) {
      JsonObject item = new JsonObject()
          .put("id", UUID.randomUUID())
          .put("pti", new JsonObject()
              .put("titleInstance", new JsonObject()
                  .put("id", packageTitle)
              )
          );
      ar.add(item);
    }
    ctx.response().putHeader("Content-Type", "application/json");
    ctx.response().setStatusCode(200);
    ctx.response().end(ar.encode());
  }

  static void getFund(RoutingContext ctx) {
    String path = ctx.request().path();
    int offset = path.lastIndexOf('/');
    UUID id = UUID.fromString(path.substring(offset + 1));
    if (id.equals(goodFundId)) {
      ctx.response().setChunked(true);
      ctx.response().putHeader("Content-Type", "application/json");
      JsonObject fund = new JsonObject();
      fund.put("ledgerId", goodLedgerId.toString());
      ctx.response().end(fund.encode());
    } else {
      ctx.response().putHeader("Content-Type", "text/plain");
      ctx.response().setStatusCode(404);
      ctx.response().end("not found");
    }
  }

  static void getLedger(RoutingContext ctx) {
    String path = ctx.request().path();
    int offset = path.lastIndexOf('/');
    UUID id = UUID.fromString(path.substring(offset + 1));
    if (id.equals(goodLedgerId)) {
      ctx.response().setChunked(true);
      ctx.response().putHeader("Content-Type", "application/json");
      JsonObject ledger = new JsonObject();
      ledger.put("fiscalYearOneId", goodFiscalYearId.toString());
      ctx.response().end(ledger.encode());
    } else {
      ctx.response().putHeader("Content-Type", "text/plain");
      ctx.response().setStatusCode(404);
      ctx.response().end("not found");
    }
  }

  static void getFiscalYear(RoutingContext ctx) {
    String path = ctx.request().path();
    int offset = path.lastIndexOf('/');
    UUID id = UUID.fromString(path.substring(offset + 1));
    if (id.equals(goodFiscalYearId)) {
      ctx.response().setChunked(true);
      ctx.response().putHeader("Content-Type", "application/json");
      JsonObject fiscalYear = new JsonObject();
      fiscalYear.put("periodStart", "2017-01-01T00:00:00Z");
      fiscalYear.put("periodEnd", "2017-12-31T23:59:59Z");
      ctx.response().end(fiscalYear.encode());
    } else {
      ctx.response().putHeader("Content-Type", "text/plain");
      ctx.response().setStatusCode(404);
      ctx.response().end("not found");
    }
  }

  static void getCompositeOrders(RoutingContext ctx) {
    String path = ctx.request().path();
    int offset = path.lastIndexOf('/');
    UUID id = UUID.fromString(path.substring(offset + 1));

    JsonObject ret = new JsonObject();
    ret.put("orderType", "One-Time");
    ctx.response().setChunked(true);
    ctx.response().putHeader("Content-Type", "application/json");
    ctx.end(ret.encode());
  }

  @BeforeClass
  public static void beforeClass(TestContext context) {
    vertx = Vertx.vertx();
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.baseURI = "http://localhost:" + MODULE_PORT;
    RestAssured.requestSpecification = new RequestSpecBuilder().build();

    Router router = Router.router(vertx);
    router.getWithRegex("/counter-reports").handler(MainVerticleTest::getCounterReports);
    router.getWithRegex("/counter-reports/[-0-9a-z]*").handler(MainVerticleTest::getCounterReport);
    router.getWithRegex("/erm/resource").handler(MainVerticleTest::getErmResource);
    router.getWithRegex("/erm/resource/[-0-9a-z]*").handler(MainVerticleTest::getErmResourceId);
    router.getWithRegex("/erm/resource/[-0-9a-z]*/entitlementOptions").handler(MainVerticleTest::getErmResourceEntitlement);
    router.getWithRegex("/erm/sas/[-0-9a-z]*").handler(MainVerticleTest::getAgreement);
    router.getWithRegex("/erm/entitlements").handler(MainVerticleTest::getEntitlements);
    router.getWithRegex("/orders/order-lines/[-0-9a-z]*").handler(MainVerticleTest::getOrderLines);
    router.getWithRegex("/invoice-storage/invoice-lines").handler(MainVerticleTest::getInvoiceLines);
    router.getWithRegex("/erm/packages/[-0-9a-z]*/content").handler(MainVerticleTest::getPackageContent);
    router.getWithRegex("/finance-storage/funds/[-0-9a-z]*").handler(MainVerticleTest::getFund);
    router.getWithRegex("/finance-storage/ledgers/[-0-9a-z]*").handler(MainVerticleTest::getLedger);
    router.getWithRegex("/finance-storage/fiscal-years/[-0-9a-z]*").handler(MainVerticleTest::getFiscalYear);
    router.getWithRegex("/orders/composite-orders/[-0-9a-z]*").handler(MainVerticleTest::getCompositeOrders);
    vertx.createHttpServer()
        .requestHandler(router)
        .listen(MOCK_PORT)
        .compose(x -> {
          DeploymentOptions deploymentOptions = new DeploymentOptions();
          deploymentOptions.setConfig(new JsonObject().put("port", Integer.toString(MODULE_PORT)));
          return vertx.deployVerticle(new MainVerticle(), deploymentOptions);
        })
        .onComplete(context.asyncAssertSuccess());
  }

  @AfterClass
  public static void afterClass(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testAdminHealth() {
    RestAssured.given()
        .get("/admin/health")
        .then().statusCode(200)
        .header("Content-Type", is("text/plain"));
  }

  @Test
  public void testGetTitlesNoInit() {
    String tenant = "testlib";
    for (int i = 0; i < 5; i++) { // would hang wo connection close
      RestAssured.given()
          .header(XOkapiHeaders.TENANT, tenant)
          .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
          .get("/eusage-reports/report-titles")
          .then().statusCode(400)
          .header("Content-Type", is("text/plain"))
          .body(containsString("testlib_mod_eusage_reports.title_entries"));
    }
  }

  @Test
  public void testGetTitlesBadCql() {
    String tenant = "testlib";
    for (int i = 0; i < 5; i++) { // would hang wo connection close
      RestAssured.given()
          .header(XOkapiHeaders.TENANT, tenant)
          .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
          .get("/eusage-reports/report-titles?query=foo=bar")
          .then().statusCode(400)
          .header("Content-Type", is("text/plain"))
          .body(containsString("Unsupported CQL index: foo"));
    }
  }

  @Test
  public void testGetTitlesNoTenant() {
    RestAssured.given()
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-titles")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(containsString("Tenant must not be null"));
  }

  @Test
  public void testGetTitlesBadLimit() {
    RestAssured.given()
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-titles?limit=-2")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(containsString("limit in location QUERY: value should be >= 0"));
  }

  @Test
  public void testPostTitlesFromCounterNoOkapiUrl() {
    String tenant = "testlib";
    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header("Content-Type", "application/json")
        .body("{}")
        .post("/eusage-reports/report-titles/from-counter")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(is("Missing " + XOkapiHeaders.URL));
  }

  @Test
  public void testPostTitlesNoInit() {
    String tenant = "testlib";
    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header("Content-Type", "application/json")
        .body(new JsonObject().put("titles", new JsonArray()).encode()) // no titles
        .post("/eusage-reports/report-titles")
        .then().statusCode(204);
  }

  @Test
  public void testPostTitlesNoInit2() {
    String tenant = "testlib";
    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header("Content-Type", "application/json")
        .body(new JsonObject().put("titles", new JsonArray()
            .add(new JsonObject()
                .put("id", UUID.randomUUID().toString())
                .put("kbTitleName", "kb title name")
                .put("kbTitleId", UUID.randomUUID().toString())
            )
        ).encode())
        .post("/eusage-reports/report-titles")
        .then().statusCode(400)
        .body(containsString("testlib_mod_eusage_reports.title_entries"));
  }

  @Test
  public void testPostTitlesBadJson() {
    String tenant = "testlib";
    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header("Content-Type", "application/json")
        .body("{")
        .post("/eusage-reports/report-titles")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(containsString("Failed to decode"));
  }

  @Test
  public void testPostTitlesBadId() {
    String tenant = "testlib";
    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header("Content-Type", "application/json")
        .body(new JsonObject().put("titles", new JsonArray()
            .add(new JsonObject()
                .put("id", "1234")
                .put("kbTitleName", "kb title name")
                .put("kbTitleId", UUID.randomUUID().toString())
            )
        ).encode())
        .post("/eusage-reports/report-titles")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(containsString("Validation error for body application/json"));
  }

  @Test
  public void testPostTitlesBadKbTitleId() {
    String tenant = "testlib";
    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header("Content-Type", "application/json")
        .body(new JsonObject().put("titles", new JsonArray()
            .add(new JsonObject()
                .put("id", UUID.randomUUID())
                .put("kbTitleName", "kb title name")
                .put("kbTitleId", "1234")
            )
        ).encode())
        .post("/eusage-reports/report-titles")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(containsString("Validation error for body application/json"));
  }

  @Test
  public void testGetTitleDataNoTenant() {
    RestAssured.given()
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/title-data")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(containsString("Tenant must not be null"));
  }

  @Test
  public void testGetReportDataNoTenant() {
    RestAssured.given()
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-data")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(containsString("Tenant must not be null"));
  }

  void tenantOp(TestContext context, String tenant, JsonObject tenantAttributes, String expectedError) {
    ExtractableResponse<Response> response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header("Content-Type", "application/json")
        .body(tenantAttributes.encode())
        .post("/_/tenant")
        .then().statusCode(201)
        .header("Content-Type", is("application/json"))
        .body("tenant", is(tenant))
        .extract();

    String location = response.header("Location");
    JsonObject tenantJob = new JsonObject(response.asString());
    context.assertEquals("/_/tenant/" + tenantJob.getString("id"), location);

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .get(location + "?wait=10000")
        .then().statusCode(200)
        .extract();

    context.assertTrue(response.path("complete"));
    context.assertEquals(expectedError, response.path("error"));

    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .delete(location)
        .then().statusCode(204);
  }

  void analyzeTitles(TestContext context, JsonObject resObject,
                     int expectTotal, int expectNumber, int expectUndef, int expectManual, int expectIgnored) {
    context.assertEquals(expectTotal, resObject.getJsonObject("resultInfo").getInteger("totalRecords"));
    context.assertEquals(0, resObject.getJsonObject("resultInfo").getJsonArray("diagnostics").size());
    JsonArray titlesAr = resObject.getJsonArray("titles");
    context.assertEquals(expectNumber, titlesAr.size());
    int noManual = 0;
    int noIgnored = 0;
    int noUndef = 0;
    for (int i = 0; i < titlesAr.size(); i++) {
      JsonObject title = titlesAr.getJsonObject(i);
      if (title.getBoolean("kbManualMatch")) {
        if (title.containsKey("kbTitleId")) {
          noManual++;
        } else {
          noIgnored++;
        }
      } else {
        if (!title.containsKey("kbTitleId")) {
          noUndef++;
        }
      }
      String publicationType = title.getString("publicationType");
      String kbTitleName = title.getString("kbTitleName");
      if (kbTitleName == null || kbTitleName.startsWith("correct")) {
        context.assertNull(publicationType);
      } else if (kbTitleName.startsWith("fake")) {
        context.assertEquals("serial", publicationType);
      } else {
        context.assertEquals("monograph", publicationType);
      }
    }
    context.assertEquals(expectUndef, noUndef);
    context.assertEquals(expectManual, noManual);
    context.assertEquals(expectIgnored, noIgnored);
  }

  @Test
  public void testFromCounterMissingOkapiUrl() {
    String tenant = "testlib";

    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header("Content-Type", "application/json")
        .body(new JsonObject()
            .put("counterReportId", goodCounterReportId)
            .encode())
        .post("/eusage-reports/report-titles/from-counter")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(is("Missing X-Okapi-Url"));
  }

  @Test
  public void testFromCounterBadId() {
    String tenant = "testlib";

    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject()
            .put("counterReportId", "1234")
            .encode())
        .post("/eusage-reports/report-titles/from-counter")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(containsString("Validation error for body application/json"));
  }

  @Test
  public void testFromAgreementNoId() {
    String tenant = "testlib";
    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject().encode())
        .post("/eusage-reports/report-data/from-agreement")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(is("Missing agreementId property"));
  }

  @Test
  public void testFromAgreementBadId() {
    String tenant = "testlib";
    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject().put("agreementId", "1234").encode())
        .post("/eusage-reports/report-data/from-agreement")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(containsString("Bad Request"));
  }

  @Test
  public void testPostTenantOK(TestContext context) {
    String tenant = "testlib";
    tenantOp(context, tenant, new JsonObject()
            .put("module_to", "mod-eusage-reports-1.0.0"), null);

    ExtractableResponse<Response> response;

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-titles?query=cql.allRecords=1")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    JsonObject resObject = new JsonObject(response.body().asString());
    analyzeTitles(context, resObject, 0, 0, 0, 0, 0);

    tenantOp(context, tenant, new JsonObject()
        .put("module_from", "mod-eusage-reports-1.0.0")
        .put("module_to", "mod-eusage-reports-1.0.1")
        .put("parameters", new JsonArray()
            .add(new JsonObject()
                .put("key", "loadReference")
                .put("value", "true")
            )
            .add(new JsonObject()
                .put("key", "loadSample")
                .put("value", "true")
            )
        ), null);

    enableGoodKbTitle = false;
    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body("{}")
        .post("/eusage-reports/report-titles/from-counter")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    analyzeTitles(context, resObject, 8, 8, 3, 0, 0);

    enableGoodKbTitle = true;
    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body("{}")
        .post("/eusage-reports/report-titles/from-counter")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    analyzeTitles(context, resObject, 8, 8, 2, 0, 0);

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body("{}")
        .post("/eusage-reports/report-titles/from-counter")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    analyzeTitles(context, resObject, 8, 8, 2, 0, 0);
    JsonArray titlesAr = resObject.getJsonArray("titles");
    int noGood = 0;
    JsonObject unmatchedTitle = null;
    for (int i = 0; i < titlesAr.size(); i++) {
      JsonObject title = titlesAr.getJsonObject(i);
      if (title.containsKey("kbTitleName")) {
        String kbTitleName = title.getString("kbTitleName");
        if ("good kb title instance name".equals(kbTitleName)) {
          noGood++;
        } else {
          context.assertEquals("fake kb title instance name", kbTitleName);
        }
      } else {
        String counterReportTitle = title.getString("counterReportTitle");
        if ("The dogs journal".equals(counterReportTitle)) {
          unmatchedTitle = title;
        } else {
          context.assertEquals("No match", counterReportTitle);
        }
      }
    }
    context.assertEquals(1, noGood);


    // put without kbTitleId kbTitleName (so title is ignored)
    JsonObject postTitleObject = new JsonObject();
    postTitleObject.put("titles", new JsonArray().add(unmatchedTitle));
    unmatchedTitle.remove("kbManualMatch"); // default is true
    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(postTitleObject.encode())
        .post("/eusage-reports/report-titles")
        .then().statusCode(204);

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-titles")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    analyzeTitles(context, resObject, 8, 8, 1, 0, 1);
    titlesAr = resObject.getJsonArray("titles");
    for (int i = 0; i < titlesAr.size(); i++) {
      JsonObject title = titlesAr.getJsonObject(i);
      String counterReportTitle = title.getString("counterReportTitle");
      if ("The cats journal".equals(counterReportTitle)) {
        context.assertEquals(goodDoiValue, title.getString("DOI"));
      } else {
        context.assertFalse(title.containsKey("DOI"), title.encodePrettily());
      }
    }

    // un-ignore the title, no longer manual match
    unmatchedTitle.put("kbManualMatch", false);
    postTitleObject.put("titles", new JsonArray().add(unmatchedTitle));
    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(postTitleObject.encode())
        .post("/eusage-reports/report-titles")
        .then().statusCode(204);

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-titles")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    analyzeTitles(context, resObject, 8, 8, 2, 0, 0);

    // put with kbTitleId kbTitleName with manual
    unmatchedTitle.put("kbManualMatch", true);
    unmatchedTitle.put("kbTitleName", "correct kb title name");
    unmatchedTitle.put("kbTitleId", UUID.randomUUID().toString());
    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(postTitleObject.encode())
        .post("/eusage-reports/report-titles")
        .then().statusCode(204);

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-titles")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    analyzeTitles(context, resObject, 8, 8, 1, 1, 0);

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-titles?query=counterReportTitle=\"cats journal\"")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    analyzeTitles(context, resObject, 1, 1, 0, 0, 0);
    context.assertEquals("The cats journal",
        resObject.getJsonArray("titles").getJsonObject(0).getString("counterReportTitle"));

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-titles?query=kbManualMatch=true")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    analyzeTitles(context, resObject, 1, 1, 0, 1, 0);

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-titles?query=kbManualMatch=false")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    analyzeTitles(context, resObject, 7, 7, 1, 0, 0);


    JsonObject n = new JsonObject();
    n.put("id", UUID.randomUUID());
    n.put("kbTitleName", "correct kb title name");
    n.put("kbTitleId", UUID.randomUUID().toString());
    postTitleObject = new JsonObject();
    postTitleObject.put("titles", new JsonArray().add(n));
    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(postTitleObject.encode())
        .post("/eusage-reports/report-titles")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(is("title " + n.getString("id") + " matches nothing"));

    // missing id
    n = new JsonObject();
    n.put("kbTitleName", "correct kb title name");
    n.put("kbTitleId", UUID.randomUUID().toString());
    postTitleObject = new JsonObject();
    postTitleObject.put("titles", new JsonArray().add(n));
    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(postTitleObject.encode())
        .post("/eusage-reports/report-titles")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(containsString("Validation error for body application/json"));

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/title-data?limit=100")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    JsonArray items = resObject.getJsonArray("data");
    context.assertEquals(60, items.size());
    int noWithPubDate = 0;
    for (int i = 0; i < items.size(); i++) {
      JsonObject item = items.getJsonObject(i);
      context.assertEquals(usageProviderId.toString(), item.getString("providerId"));
      String pubDate = item.getString("publicationDate");
      if (pubDate != null) {
        noWithPubDate++;
        String title = item.getString("counterReportTitle");
        if ("The dogs journal".equals(title)) {
          context.assertEquals(pubYearSample + "-01-01", pubDate);
        } else {
          context.assertEquals(pubDateSample, pubDate);
        }
      }
    }
    context.assertEquals(30, noWithPubDate);

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject()
            .put("counterReportId", goodCounterReportId)
            .encode())
        .post("/eusage-reports/report-titles/from-counter")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    context.assertEquals(9, resObject.getJsonArray("titles").size());

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject()
            .put("providerId", usageProviderId)
            .encode())
        .post("/eusage-reports/report-titles/from-counter")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    context.assertEquals(9, resObject.getJsonArray("titles").size());

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject()
            .put("providerId", UUID.randomUUID().toString())
            .encode())
        .post("/eusage-reports/report-titles/from-counter")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    context.assertEquals(9, resObject.getJsonArray("titles").size());

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-titles?counterReportId=" + goodCounterReportId)
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    titlesAr = resObject.getJsonArray("titles");
    context.assertEquals(4, titlesAr.size());

    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-titles?counterReportId=x")
        .then().statusCode(400)
        .body(is("Invalid UUID string: x"));

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-titles?providerId=" + usageProviderId)
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    titlesAr = resObject.getJsonArray("titles");
    context.assertEquals(9, titlesAr.size());

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-titles?providerId=" + usageProviderId + "&query=kbTitleId=" + goodKbTitleId)
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    titlesAr = resObject.getJsonArray("titles");
    context.assertEquals(1, titlesAr.size());
    context.assertEquals(goodKbTitleId.toString(), titlesAr.getJsonObject(0).getString("kbTitleId"));

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-titles?query=kbTitleId<>\"\"")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    titlesAr = resObject.getJsonArray("titles");
    context.assertEquals(8, titlesAr.size());

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-titles?query=kbTitleId=\"\"")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    titlesAr = resObject.getJsonArray("titles");
    context.assertEquals(1, titlesAr.size());

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-titles?providerId=" + UUID.randomUUID())
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    titlesAr = resObject.getJsonArray("titles");
    context.assertEquals(0, titlesAr.size());

    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject()
            .put("counterReportId", badJsonCounterReportId)
            .encode())
        .post("/eusage-reports/report-titles/from-counter")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(containsString("returned bad JSON"));

    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject()
            .put("counterReportId", badStatusCounterReportId)
            .encode())
        .post("/eusage-reports/report-titles/from-counter")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(containsString("returned status code 403"));

    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject()
            .put("counterReportId", UUID.randomUUID()) // unknown ID
            .encode())
        .post("/eusage-reports/report-titles/from-counter")
        .then().statusCode(404);

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-data")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    context.assertEquals(0, resObject.getJsonArray("data").size());

    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-data/status/" + UUID.randomUUID())
        .then().statusCode(404);

    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject()
            .put("agreementId", UUID.randomUUID()) // unknown ID
            .encode())
        .post("/eusage-reports/report-data/from-agreement")
        .then().statusCode(404);

    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject()
            .put("agreementId", badJsonAgreementId)
            .encode())
        .post("/eusage-reports/report-data/from-agreement")
        .then().statusCode(400)
        .body(containsString("Failed to decode"));

    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject()
            .put("agreementId", badStatusAgreementId)
            .encode())
        .post("/eusage-reports/report-data/from-agreement")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(containsString("returned status code 403"));

    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject()
            .put("agreementId", badStatusAgreementId2)
            .encode())
        .post("/eusage-reports/report-data/from-agreement")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(containsString("returned status code 500"));

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject()
            .put("agreementId", goodAgreementId)
            .encode())
        .post("/eusage-reports/report-data/from-agreement")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    context.assertEquals(4, resObject.getInteger("reportLinesCreated"));

    String b = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-data/status/" + goodAgreementId)
        .then().statusCode(200).extract().body().asString();
    log.info("AD: {}", b);

    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-data/status/" + goodAgreementId)
        .then().statusCode(200)
        .body("id", is(goodAgreementId.toString()))
        .body("lastUpdated", Matchers.not(isEmptyOrNullString()))
        .body("active", is(false));

    // running the from-agreement twice (wiping out the ond one above)
    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject()
            .put("agreementId", goodAgreementId)
            .encode())
        .post("/eusage-reports/report-data/from-agreement")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    context.assertEquals(4, resObject.getInteger("reportLinesCreated"));

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject()
            .put("counterReportId", otherCounterReportId)
            .encode())
        .post("/eusage-reports/report-titles/from-counter?offset=11")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    context.assertEquals(4, resObject.getJsonArray("titles").size());
    context.assertEquals(15, resObject.getJsonObject("resultInfo").getInteger("totalRecords"));
    context.assertEquals(0, resObject.getJsonObject("resultInfo").getJsonArray("diagnostics").size());

    response = RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .get("/eusage-reports/report-data")
        .then().statusCode(200)
        .header("Content-Type", is("application/json"))
        .extract();
    resObject = new JsonObject(response.body().asString());
    items = resObject.getJsonArray("data");
    context.assertEquals(4, items.size());
    int noPackages = 0;
    for (int i = 0; i < items.size(); i++) {
      JsonObject item = items.getJsonObject(i);
      String type =  item.getString("type");
      if ("package".equals(type)) {
        context.assertEquals(goodPackageId.toString(), item.getString("kbPackageId"));
        context.assertFalse(item.containsKey("kbTitleId"));
        noPackages++;
      } else {
        context.assertEquals("serial", type);
        context.assertFalse(item.containsKey("kbPackageId"));
        context.assertTrue(item.containsKey("kbTitleId"));
        context.assertEquals("One-Time", item.getString("orderType"));
        String invoiceNumber = item.getString("invoiceNumber");
        context.assertTrue("0".equals(invoiceNumber) || "1".equals(invoiceNumber));
        context.assertEquals(POLINE_NUMBER_SAMPLE, item.getString("poLineNumber"));
      }
    }
    context.assertEquals(1, noPackages);

    orderLinesCurrencies.clear();
    orderLinesCurrencies.add("DKK");
    orderLinesCurrencies.add("EUR");

    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header(XOkapiHeaders.URL, "http://localhost:" + MOCK_PORT)
        .header("Content-Type", "application/json")
        .body(new JsonObject()
            .put("agreementId", goodAgreementId)
            .encode())
        .post("/eusage-reports/report-data/from-agreement")
        .then().statusCode(400)
        .header("Content-Type", is("text/plain"))
        .body(containsString("Mixed currencies"));

    // disable
    tenantOp(context, tenant,
        new JsonObject().put("module_from", "mod-eusage-reports-1.0.0"), null);

    // purge
    RestAssured.given()
        .header(XOkapiHeaders.TENANT, tenant)
        .header("Content-Type", "application/json")
        .body(
            new JsonObject()
                .put("module_from", "mod-eusage-reports-1.0.0")
                .put("purge", true).encode())
        .post("/_/tenant")
        .then().statusCode(204);
  }
}
