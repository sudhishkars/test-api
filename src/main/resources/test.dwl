%dw 2.0
output application/json
var dscMap = {
    EBT: "EBT",
    Interest: "Interest",
    EBITDA: "EBITDA"
}
var extraMap = {
    TotalLiabilities: "TotalLiabilities",
    OperatingProfitMargin: "OperatingProfitMargin"
}
var olcMap = {
    Revenue: "Revenue"
}

var statementDates = payload.dsc.statementdate filter $ != "Pro Forma"
var recent = max(statementDates)
var prev = min(statementDates)

fun createMetrics (keyMap, kpis) = (
    do {
        var kpiByDate = kpis groupBy $.statementdate
        ---
        keyMap pluck ((value, key) -> {
            name: value,
            recent: kpiByDate[recent][value][0] default null,
            previous: kpiByDate[prev][value][0] default null,
            proforma: kpiByDate["Pro Forma"][value][0] default null
        })
    }
)

fun createOlcMetrics (kpi) = (
    olcMap pluck ((value, key) -> {
        name: value,
        recent: kpi[key] default null
    })
)
---
{
    recent: recent,
    prev: prev,
    metrics: createMetrics(dscMap, payload.dsc) ++ createMetrics(extraMap, payload.extra) ++ createOlcMetrics(payload.olc)

}
