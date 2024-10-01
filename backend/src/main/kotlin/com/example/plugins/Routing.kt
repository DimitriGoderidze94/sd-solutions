package com.example.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import java.io.BufferedReader
import java.io.InputStreamReader

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }

        // Endpoint to initiate a scan
        post("/api/scan") {
            val request = call.receiveParameters()
            val domain = request["domain"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Domain not provided")

            // Run theHarvester using Docker
            try {
                val command = "docker run --rm simonthomas/theharvester -d $domain -b all"
                val process = Runtime.getRuntime().exec(command)

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }

                process.waitFor()
                call.respond(HttpStatusCode.Created, "Scan completed: \n$output")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error running theHarvester: ${e.message}")
            }
        }

        // Endpoint to get past scans (same as before)
        get("/api/scans") {
            val pastScans = """
                Past Scans:
                1. Domain: google.com, Start Time: 2024-10-01T10:00:00Z, End Time: 2024-10-01T10:05:00Z
                2. Domain: example.com, Start Time: 2024-10-01T11:00:00Z, End Time: 2024-10-01T11:05:00Z
            """.trimIndent()

            call.respond(HttpStatusCode.OK, pastScans)
        }
    }
}