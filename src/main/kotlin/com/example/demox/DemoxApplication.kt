package com.example.demox

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.sql.ResultSet
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@SpringBootApplication
class DemoxApplication

fun main(args: Array<String>) {
    runApplication<DemoxApplication>(*args)
}

@Validated
@RestController
class TestController {

    @Autowired
    lateinit var databasePort: databasePort

    /*
    can have request of datetime string like eg:
    - 2020-10-21T06:07:07+07:00
    - 2020-10-21T08:07:07Z
     */
    @PostMapping(
        value = ["/waktu"],
        consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun addWaktu(@RequestBody @Valid req: List<Waktu>): ResponseEntity<Any?> {

        println("payload:\n$req")
        return ResponseEntity.ok("ok")

//        databasePort.insertWaktu(req)
//        val data = databasePort.getWaktu(req.id)
//        println("data:\n$data")
//        return ResponseEntity.ok("ok, waktu: $data")
    }
}

data class Waktu(
    @field:JsonProperty("id")
    val id: Int,

    @field:NotEmpty(message = "note tidak boleh kosong")
    val note: String,

    @field:NotNull(message = "ukuran tidak boleh kosong")
    val ukuran: Int,

    @field:JsonProperty("kapan")
    @field:JsonDeserialize(using = KapanToUTC::class)
    val kapan: LocalDateTime
)

@Service
class databasePort {

    @Autowired
    lateinit var namedJdbcTemplate: NamedParameterJdbcTemplate

    fun insertWaktu(waktu: Waktu) {
        val sql = """
            INSERT INTO waktu(id, kapan)
            VALUES(:id, :kapan);
        """.trimIndent()

        val parameters = MapSqlParameterSource()
        parameters.addValue("id", waktu.id)
        parameters.addValue("kapan", waktu.kapan)
        namedJdbcTemplate.update(sql, parameters)
    }

    fun getWaktu(id: Int): Waktu? {
        val sql = """
           SELECT * FROM waktu WHERE id = :id;
        """.trimIndent()
        val parameterSource = MapSqlParameterSource()
        parameterSource.addValue("id", id)
        val result = namedJdbcTemplate.query(sql, parameterSource, WaktuRowMapper())
        return if (result.size > 0) {
            result[0]
        } else {
            null
        }
    }
}

class WaktuRowMapper : RowMapper<Waktu> {
    override fun mapRow(rs: ResultSet, rowNum: Int): Waktu {
        return Waktu(
            id = rs.getInt("ID"),
            kapan = rs.getTimestamp("KAPAN").toLocalDateTime(),
            note = "aaa",
            ukuran = 1
        )
    }
}

class KapanToUTC : JsonDeserializer<LocalDateTime>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDateTime? {
        val node: JsonNode = p.codec.readTree(p)
        val kapanAt = node.textValue()
        return if (kapanAt.isNotEmpty()) {
            OffsetDateTime.parse(kapanAt, DateTimeFormatter.ISO_DATE_TIME)
                .withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
        } else {
            null
        }
    }
}
