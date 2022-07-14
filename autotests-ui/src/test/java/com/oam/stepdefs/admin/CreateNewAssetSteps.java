package com.oam.stepdefs.admin;
import com.oam.pages.admin.CreateNewAssetPage;
import io.cucumber.datatable.DataTable;
import com.oam.stepdefs.AbstractStep;
import io.cucumber.java8.En;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class CreateNewAssetSteps extends AbstractStep implements En {
    @Autowired
    private CreateNewAssetPage createNewAssetPage;
    public CreateNewAssetSteps() {
        And("^the user creates a new asset$",(DataTable dataTable)->{
            Map<String, String> assetDetails = dataTable.asMaps().get(0);
            createNewAssetPage.inputAssetDetails(assetDetails.get("name"),assetDetails.get("specification"),assetDetails.get("state"));
        });
    }
}
