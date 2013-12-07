package net.rptools.asset.intern.supplier;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.rptools.asset.Asset;
import net.rptools.asset.AssetListener;
import net.rptools.asset.AssetSupplier;
import net.rptools.asset.intern.AssetImpl;

import org.junit.Before;
import org.junit.Test;

public class DefaultSupplierSelectionStrategyTest {
    private String assetId; // for async tests
    private SortedSet<AssetSupplier> assetSuppliers;
    @Before
    public void setup() {
        assetId = null;
        // External comparator
        assetSuppliers = new TreeSet<AssetSupplier>(new Comparator<AssetSupplier>() {
            @Override
            public int compare(AssetSupplier high, AssetSupplier low) {
                return low.getPriority() - high.getPriority();
            }
        });
    }

    @Test
    public void testReadTypePriority() {
        AssetSupplier supplier1 = createMock("Supplier1", HttpAssetSupplier.class);
        AssetSupplier supplier2 = createMock("Supplier2", FileAssetSupplier.class);
        AssetSupplier supplier3 = createMock("Supplier3", HttpAssetSupplier.class);
        AssetImpl ret = new AssetImpl(null);
        expect(supplier1.getPriority()).andReturn(1).anyTimes();
        expect(supplier2.getPriority()).andReturn(3).anyTimes();
        expect(supplier3.getPriority()).andReturn(2).anyTimes();
        expect(supplier2.has("1")).andReturn(true);
        expect(supplier2.get("1", null)).andReturn(ret);

        replay(supplier1, supplier2, supplier3);
        assetSuppliers.add(supplier1);
        assetSuppliers.add(supplier2);
        assetSuppliers.add(supplier3);
        Asset test = DefaultSupplierSelectionStrategy.getAssetByStrategy(assetSuppliers , "1", null, false);
        assertEquals(test, ret);
        verify(supplier1, supplier2, supplier3);
    }

    @Test
    public void testReadPrioPriority() {
        AssetSupplier supplier1 = createMock("Supplier1", HttpAssetSupplier.class);
        AssetSupplier supplier2 = createMock("Supplier2", HttpAssetSupplier.class);
        AssetSupplier supplier3 = createMock("Supplier3", HttpAssetSupplier.class);
        AssetImpl ret = new AssetImpl(null);
        expect(supplier1.getPriority()).andReturn(1).anyTimes();
        expect(supplier2.getPriority()).andReturn(3).anyTimes();
        expect(supplier3.getPriority()).andReturn(2).anyTimes();
        expect(supplier2.has("2")).andReturn(true);
        expect(supplier2.get("2", null)).andReturn(ret);

        replay(supplier1, supplier2, supplier3);
        assetSuppliers.add(supplier1);
        assetSuppliers.add(supplier2);
        assetSuppliers.add(supplier3);
        Asset test = DefaultSupplierSelectionStrategy.getAssetByStrategy(assetSuppliers , "2", null, false);
        assertEquals(test, ret);
        verify(supplier1, supplier2, supplier3);
    }

    @Test
    public void testReadPrioPriorityWithGap() {
        AssetSupplier supplier1 = createMock("Supplier1", HttpAssetSupplier.class);
        AssetSupplier supplier2 = createMock("Supplier2", HttpAssetSupplier.class);
        AssetSupplier supplier3 = createMock("Supplier3", HttpAssetSupplier.class);
        AssetImpl ret = new AssetImpl(null);
        expect(supplier1.getPriority()).andReturn(1).anyTimes();
        expect(supplier2.getPriority()).andReturn(3).anyTimes();
        expect(supplier3.getPriority()).andReturn(2).anyTimes();
        expect(supplier2.has("3")).andReturn(false);
        expect(supplier3.has("3")).andReturn(true);
        expect(supplier3.get("3", null)).andReturn(ret);

        replay(supplier1, supplier2, supplier3);
        assetSuppliers.add(supplier1);
        assetSuppliers.add(supplier2);
        assetSuppliers.add(supplier3);
        Asset test = DefaultSupplierSelectionStrategy.getAssetByStrategy(assetSuppliers , "3", null, false);
        assertEquals(test, ret);
        verify(supplier1, supplier2, supplier3);
    }

    @Test
    public void testWriteTypePriority() throws InterruptedException {
        AssetSupplier supplier1 = createMock("Supplier1", HttpAssetSupplier.class);
        AssetSupplier supplier2 = createMock("Supplier2", FileAssetSupplier.class);
        AssetSupplier supplier3 = createMock("Supplier3", HttpAssetSupplier.class);
        AssetImpl asset = new AssetImpl(true);
        final CountDownLatch latch = new CountDownLatch(1);
        AssetListener listener = new AssetListener() {
            @Override
            public void notify(String id, Asset obj) {
                latch.countDown();
                assetId = id;
            }
            @Override
            public void notifyPartial(String id, double completed) {
            }
        };
        expect(supplier1.getPriority()).andReturn(1).anyTimes();
        expect(supplier2.getPriority()).andReturn(3).anyTimes();
        expect(supplier3.getPriority()).andReturn(2).anyTimes();
        expect(supplier2.canCreate(Boolean.class)).andReturn(true);
        expect(supplier2.create(asset)).andReturn("4");

        replay(supplier1, supplier2, supplier3);
        assetSuppliers.add(supplier1);
        assetSuppliers.add(supplier2);
        assetSuppliers.add(supplier3);
        DefaultSupplierSelectionStrategy.createAsset(assetSuppliers , asset, listener, false);
        latch.await(1, TimeUnit.SECONDS);
        assertEquals(assetId, "4");
        verify(supplier1, supplier2, supplier3);
    }

    @Test
    public void testWritePrioPriority() throws InterruptedException {
        AssetSupplier supplier1 = createMock("Supplier1", FileAssetSupplier.class);
        AssetSupplier supplier2 = createMock("Supplier2", FileAssetSupplier.class);
        AssetSupplier supplier3 = createMock("Supplier3", FileAssetSupplier.class);
        AssetImpl asset = new AssetImpl(true);
        final CountDownLatch latch = new CountDownLatch(1);
        AssetListener listener = new AssetListener() {
            @Override
            public void notify(String id, Asset obj) {
                latch.countDown();
                assetId = id;
            }
            @Override
            public void notifyPartial(String id, double completed) {
            }
        };
        expect(supplier1.getPriority()).andReturn(1).anyTimes();
        expect(supplier2.getPriority()).andReturn(3).anyTimes();
        expect(supplier3.getPriority()).andReturn(2).anyTimes();
        expect(supplier2.canCreate(Boolean.class)).andReturn(true);
        expect(supplier2.create(asset)).andReturn("5");

        replay(supplier1, supplier2, supplier3);
        assetSuppliers.add(supplier1);
        assetSuppliers.add(supplier2);
        assetSuppliers.add(supplier3);
        DefaultSupplierSelectionStrategy.createAsset(assetSuppliers , asset, listener, false);
        latch.await(1, TimeUnit.SECONDS);
        assertEquals(assetId, "5");
        verify(supplier1, supplier2, supplier3);
    }

    @Test
    public void testWritePrioPriorityWithGap() throws InterruptedException {
        AssetSupplier supplier1 = createMock("Supplier1", FileAssetSupplier.class);
        AssetSupplier supplier2 = createMock("Supplier2", FileAssetSupplier.class);
        AssetSupplier supplier3 = createMock("Supplier3", FileAssetSupplier.class);
        AssetImpl asset = new AssetImpl(true);
        final CountDownLatch latch = new CountDownLatch(1);
        AssetListener listener = new AssetListener() {
            @Override
            public void notify(String id, Asset obj) {
                latch.countDown();
                assetId = id;
            }
            @Override
            public void notifyPartial(String id, double completed) {
            }
        };
        expect(supplier1.getPriority()).andReturn(1).anyTimes();
        expect(supplier2.getPriority()).andReturn(3).anyTimes();
        expect(supplier3.getPriority()).andReturn(2).anyTimes();
        expect(supplier2.canCreate(Boolean.class)).andReturn(false);
        expect(supplier3.canCreate(Boolean.class)).andReturn(true);
        expect(supplier3.create(asset)).andReturn("6");

        replay(supplier1, supplier2, supplier3);
        assetSuppliers.add(supplier1);
        assetSuppliers.add(supplier2);
        assetSuppliers.add(supplier3);
        DefaultSupplierSelectionStrategy.createAsset(assetSuppliers , asset, listener, false);
        latch.await(1, TimeUnit.SECONDS);
        assertEquals(assetId, "6");
        verify(supplier1, supplier2, supplier3);
    }
}
