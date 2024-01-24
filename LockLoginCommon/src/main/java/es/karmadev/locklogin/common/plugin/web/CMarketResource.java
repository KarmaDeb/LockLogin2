package es.karmadev.locklogin.common.plugin.web;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.locklogin.api.plugin.marketplace.Category;
import es.karmadev.locklogin.api.plugin.marketplace.resource.MarketResource;
import es.karmadev.locklogin.api.plugin.marketplace.resource.ResourceDownload;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.net.URL;

@Value(staticConstructor = "of")
@Getter
public class CMarketResource implements MarketResource {

    int id;
    Category category;
    String name;
    String description;
    String version;
    String publisher;
    int downloads;

    @FieldNameConstants.Exclude
    String downloadName;
    @FieldNameConstants.Exclude
    long downloadSize;
    @FieldNameConstants.Exclude
    URL downloadURL;

    /**
     * Get the resource download
     * information
     */
    @Override
    public ResourceDownload getDownload() {
        DownloadHandle handle = DownloadHandle.empty();
        if (downloadSize > 0 && !ObjectUtils.areNullOrEmpty(false, downloadName, downloadURL)) {
            handle = DownloadHandle.of(downloadName, downloadSize, downloadURL, this);
        }

        return handle;
    }
}
