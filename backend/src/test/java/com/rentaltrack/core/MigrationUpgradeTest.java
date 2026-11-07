package com.rentaltrack.core;

import static org.assertj.core.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class MigrationUpgradeTest {

  @Test
  void v10DataAndForeignKeysSurviveV11AlongsideTheNewEmptyModel() {
    String main = System.getenv().getOrDefault(
      "SPRING_DATASOURCE_URL",
      "jdbc:mysql://localhost:3306/rentaltrack_test?useSSL=false&allowPublicKeyRetrieval=true"
    );
    String url = System.getenv().getOrDefault(
      "RENTALTRACK_UPGRADE_URL",
      main.replace("/rentaltrack_test", "/rentaltrack_upgrade_test")
    );
    if (url.equals(main)) throw new IllegalStateException(
      "RENTALTRACK_UPGRADE_URL must name a separate disposable database"
    );
    String user = System.getenv().getOrDefault(
        "SPRING_DATASOURCE_USERNAME",
        "rentaltrack"
      ),
      password = System.getenv().getOrDefault(
        "SPRING_DATASOURCE_PASSWORD",
        "rentaltrack"
      );
    var source = new DriverManagerDataSource(url, user, password);
    var jdbc = new JdbcTemplate(source);
    if (
      !"rentaltrack_upgrade_test".equals(
        jdbc.queryForObject("SELECT DATABASE()", String.class)
      )
    ) throw new IllegalStateException(
      "Upgrade test may only clean rentaltrack_upgrade_test"
    );
    Flyway.configure().dataSource(source).cleanDisabled(false).load().clean();
    Flyway.configure().dataSource(source).target("10").load().migrate();
    // A dedicated fixture DB is created afresh by CI/local validation, never an application DB.
    jdbc.update(
      "INSERT INTO member_account(id,email,name,status,password_hash) VALUES (1,'old@example.com','Legacy member','ACTIVE','preserved-hash')"
    );
    jdbc.update(
      "INSERT INTO rental_location(id,code,name,address,latitude,longitude) VALUES (1,'OLD','Old location','Old address',37,127)"
    );
    jdbc.update(
      "INSERT INTO product_category(id,code,name) VALUES (1,'OLD','Old category')"
    );
    jdbc.update(
      "INSERT INTO rental_product(id,asset_code,display_name,category_id,current_location_id) VALUES (1,'OLD-ASSET','Old item',1,1)"
    );
    jdbc.update(
      "INSERT INTO rental_contract(id,contract_code,member_id,rental_product_id,pickup_location_id,status,requested_at) VALUES (1,'OLD-CONTRACT',1,1,1,'REQUESTED',CURRENT_TIMESTAMP)"
    );
    jdbc.update(
      "INSERT INTO condition_record(id,rental_contract_id,record_type,summary,recorded_at) VALUES (1,1,'BEFORE_RENTAL','Old condition',CURRENT_TIMESTAMP)"
    );
    jdbc.update(
      "INSERT INTO condition_image(condition_record_id,storage_key,capture_order) VALUES (1,'old/photo.png',1)"
    );
    Flyway.configure().dataSource(source).load().migrate();
    assertThat(
      jdbc.queryForObject(
        "SELECT contract_code FROM legacy_rental_contract WHERE id=1",
        String.class
      )
    ).isEqualTo("OLD-CONTRACT");
    assertThat(
      jdbc.queryForObject(
        "SELECT summary FROM legacy_condition_record WHERE id=1",
        String.class
      )
    ).isEqualTo("Old condition");
    assertThat(
      jdbc.queryForObject(
        "SELECT COUNT(*) FROM condition_image p JOIN legacy_condition_record c ON c.id=p.condition_record_id JOIN legacy_rental_contract r ON r.id=c.rental_contract_id",
        Integer.class
      )
    ).isEqualTo(1);
    assertThat(
      jdbc.queryForObject(
        "SELECT password_hash FROM member_account WHERE id=1",
        String.class
      )
    ).isEqualTo("preserved-hash");
    assertThat(
      jdbc.queryForObject("SELECT COUNT(*) FROM rental_contract", Integer.class)
    ).isZero();
    assertThat(
      jdbc.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history WHERE success=1",
        Integer.class
      )
    ).isEqualTo(11);
  }
}
