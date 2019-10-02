/**
 * This class is machine-made by {de.kapsi.net.daap.tools.ChunkFactoryGenerator}!
 * It is needed because Reflection cannot list the classes of a package so that we
 * must pre-create a such list manually. This file must be rebuild whenever a class
 * is removed or a class is added to the {@see de.kapsi.net.daap.chunks.impl} package.
 */
package de.kapsi.net.daap.chunks;

import java.util.HashMap;
import java.util.Map;

import de.kapsi.net.daap.DaapUtil;

public final class ChunkFactory {

    private final Map<Integer, Class<? extends Chunk>> map = new HashMap<Integer, Class<? extends Chunk>>();

    public ChunkFactory() {
        map.put(new Integer(0x6D736175), de.kapsi.net.daap.chunks.impl.AuthenticationMethod.class); //msau
        map.put(new Integer(0x6D736173), de.kapsi.net.daap.chunks.impl.AuthenticationSchemes.class); //msas
        map.put(new Integer(0x6D62636C), de.kapsi.net.daap.chunks.impl.Bag.class); //mbcl
        map.put(new Integer(0x6162706C), de.kapsi.net.daap.chunks.impl.BasePlaylist.class); //abpl
        map.put(new Integer(0x6162616C), de.kapsi.net.daap.chunks.impl.BrowseAlbumListing.class); //abal
        map.put(new Integer(0x61626172), de.kapsi.net.daap.chunks.impl.BrowseArtistListing.class); //abar
        map.put(new Integer(0x61626370), de.kapsi.net.daap.chunks.impl.BrowseComposerListing.class); //abcp
        map.put(new Integer(0x6162676E), de.kapsi.net.daap.chunks.impl.BrowseGenreListing.class); //abgn
        map.put(new Integer(0x6D636F6E), de.kapsi.net.daap.chunks.impl.Container.class); //mcon
        map.put(new Integer(0x6D637463), de.kapsi.net.daap.chunks.impl.ContainerCount.class); //mctc
        map.put(new Integer(0x6D637469), de.kapsi.net.daap.chunks.impl.ContainerItemId.class); //mcti
        map.put(new Integer(0x6D636E61), de.kapsi.net.daap.chunks.impl.ContentCodesName.class); //mcna
        map.put(new Integer(0x6D636E6D), de.kapsi.net.daap.chunks.impl.ContentCodesNumber.class); //mcnm
        map.put(new Integer(0x6D636372), de.kapsi.net.daap.chunks.impl.ContentCodesResponse.class); //mccr
        map.put(new Integer(0x6D637479), de.kapsi.net.daap.chunks.impl.ContentCodesType.class); //mcty
        map.put(new Integer(0x6170726F), de.kapsi.net.daap.chunks.impl.DaapProtocolVersion.class); //apro
        map.put(new Integer(0x6162726F), de.kapsi.net.daap.chunks.impl.DatabaseBrowse.class); //abro
        map.put(new Integer(0x6D736463), de.kapsi.net.daap.chunks.impl.DatabaseCount.class); //msdc
        map.put(new Integer(0x61706C79), de.kapsi.net.daap.chunks.impl.DatabasePlaylists.class); //aply
        map.put(new Integer(0x61646273), de.kapsi.net.daap.chunks.impl.DatabaseSongs.class); //adbs
        map.put(new Integer(0x6D75646C), de.kapsi.net.daap.chunks.impl.DeletedIdListing.class); //mudl
        map.put(new Integer(0x6D64636C), de.kapsi.net.daap.chunks.impl.Dictionary.class); //mdcl
        map.put(new Integer(0x6D70726F), de.kapsi.net.daap.chunks.impl.DmapProtocolVersion.class); //mpro
        map.put(new Integer(0x668D6368), de.kapsi.net.daap.chunks.impl.HasChildContainers.class); //f?ch
        map.put(new Integer(0x61654856), de.kapsi.net.daap.chunks.impl.HasVideo.class); //aeHV
        map.put(new Integer(0x6D696D63), de.kapsi.net.daap.chunks.impl.ItemCount.class); //mimc
        map.put(new Integer(0x6D696964), de.kapsi.net.daap.chunks.impl.ItemId.class); //miid
        map.put(new Integer(0x6D696B64), de.kapsi.net.daap.chunks.impl.ItemKind.class); //mikd
        map.put(new Integer(0x6D696E6D), de.kapsi.net.daap.chunks.impl.ItemName.class); //minm
        map.put(new Integer(0x61654149), de.kapsi.net.daap.chunks.impl.ITMSArtistId.class); //aeAI
        map.put(new Integer(0x61654349), de.kapsi.net.daap.chunks.impl.ITMSComposerId.class); //aeCI
        map.put(new Integer(0x61654749), de.kapsi.net.daap.chunks.impl.ITMSGenreId.class); //aeGI
        map.put(new Integer(0x61655049), de.kapsi.net.daap.chunks.impl.ITMSPlaylistId.class); //aePI
        map.put(new Integer(0x61655349), de.kapsi.net.daap.chunks.impl.ITMSSongId.class); //aeSI
        map.put(new Integer(0x61655346), de.kapsi.net.daap.chunks.impl.ITMSStorefrontId.class); //aeSF
        map.put(new Integer(0x6D6C636C), de.kapsi.net.daap.chunks.impl.Listing.class); //mlcl
        map.put(new Integer(0x6D6C6974), de.kapsi.net.daap.chunks.impl.ListingItem.class); //mlit
        map.put(new Integer(0x6D736C72), de.kapsi.net.daap.chunks.impl.LoginRequired.class); //mslr
        map.put(new Integer(0x6D6C6F67), de.kapsi.net.daap.chunks.impl.LoginResponse.class); //mlog
        map.put(new Integer(0x61655356), de.kapsi.net.daap.chunks.impl.MusicSharingVersion.class); //aeSV
        map.put(new Integer(0x61654E56), de.kapsi.net.daap.chunks.impl.NormVolume.class); //aeNV
        map.put(new Integer(0x6D70636F), de.kapsi.net.daap.chunks.impl.ParentContainerId.class); //mpco
        map.put(new Integer(0x6D706572), de.kapsi.net.daap.chunks.impl.PersistentId.class); //mper
        map.put(new Integer(0x6170726D), de.kapsi.net.daap.chunks.impl.PlaylistRepeatMode.class); //aprm
        map.put(new Integer(0x6170736D), de.kapsi.net.daap.chunks.impl.PlaylistShuffleMode.class); //apsm
        map.put(new Integer(0x6170736F), de.kapsi.net.daap.chunks.impl.PlaylistSongs.class); //apso
        map.put(new Integer(0x61655043), de.kapsi.net.daap.chunks.impl.Podcast.class); //aePC
        map.put(new Integer(0x61655050), de.kapsi.net.daap.chunks.impl.PodcastPlaylist.class); //aePP
        map.put(new Integer(0x61727376), de.kapsi.net.daap.chunks.impl.Resolve.class); //arsv
        map.put(new Integer(0x61726966), de.kapsi.net.daap.chunks.impl.ResolveInfo.class); //arif
        map.put(new Integer(0x6D72636F), de.kapsi.net.daap.chunks.impl.ReturnedCount.class); //mrco
        map.put(new Integer(0x61766462), de.kapsi.net.daap.chunks.impl.ServerDatabases.class); //avdb
        map.put(new Integer(0x6D737276), de.kapsi.net.daap.chunks.impl.ServerInfoResponse.class); //msrv
        map.put(new Integer(0x6D757372), de.kapsi.net.daap.chunks.impl.ServerRevision.class); //musr
        map.put(new Integer(0x6D6C6964), de.kapsi.net.daap.chunks.impl.SessionId.class); //mlid
        map.put(new Integer(0x61655350), de.kapsi.net.daap.chunks.impl.SmartPlaylist.class); //aeSP
        map.put(new Integer(0x6173616C), de.kapsi.net.daap.chunks.impl.SongAlbum.class); //asal
        map.put(new Integer(0x61736172), de.kapsi.net.daap.chunks.impl.SongArtist.class); //asar
        map.put(new Integer(0x61736274), de.kapsi.net.daap.chunks.impl.SongBeatsPerMinute.class); //asbt
        map.put(new Integer(0x61736272), de.kapsi.net.daap.chunks.impl.SongBitrate.class); //asbr
        map.put(new Integer(0x61736374), de.kapsi.net.daap.chunks.impl.SongCategory.class); //asct
        map.put(new Integer(0x61736373), de.kapsi.net.daap.chunks.impl.SongCodecSubtype.class); //ascs
        map.put(new Integer(0x61736364), de.kapsi.net.daap.chunks.impl.SongCodecType.class); //ascd
        map.put(new Integer(0x6173636D), de.kapsi.net.daap.chunks.impl.SongComment.class); //ascm
        map.put(new Integer(0x6173636F), de.kapsi.net.daap.chunks.impl.SongCompilation.class); //asco
        map.put(new Integer(0x61736370), de.kapsi.net.daap.chunks.impl.SongComposer.class); //ascp
        map.put(new Integer(0x6173636E), de.kapsi.net.daap.chunks.impl.SongContentDescription.class); //ascn
        map.put(new Integer(0x61736372), de.kapsi.net.daap.chunks.impl.SongContentRating.class); //ascr
        map.put(new Integer(0x6173646B), de.kapsi.net.daap.chunks.impl.SongDataKind.class); //asdk
        map.put(new Integer(0x6173756C), de.kapsi.net.daap.chunks.impl.SongDataUrl.class); //asul
        map.put(new Integer(0x61736461), de.kapsi.net.daap.chunks.impl.SongDateAdded.class); //asda
        map.put(new Integer(0x6173646D), de.kapsi.net.daap.chunks.impl.SongDateModified.class); //asdm
        map.put(new Integer(0x61736474), de.kapsi.net.daap.chunks.impl.SongDescription.class); //asdt
        map.put(new Integer(0x61736462), de.kapsi.net.daap.chunks.impl.SongDisabled.class); //asdb
        map.put(new Integer(0x61736463), de.kapsi.net.daap.chunks.impl.SongDiscCount.class); //asdc
        map.put(new Integer(0x6173646E), de.kapsi.net.daap.chunks.impl.SongDiscNumber.class); //asdn
        map.put(new Integer(0x61736571), de.kapsi.net.daap.chunks.impl.SongEqPreset.class); //aseq
        map.put(new Integer(0x6173666D), de.kapsi.net.daap.chunks.impl.SongFormat.class); //asfm
        map.put(new Integer(0x6173676E), de.kapsi.net.daap.chunks.impl.SongGenre.class); //asgn
        map.put(new Integer(0x61677270), de.kapsi.net.daap.chunks.impl.SongGrouping.class); //agrp
        map.put(new Integer(0x61736B79), de.kapsi.net.daap.chunks.impl.SongKeywords.class); //asky
        map.put(new Integer(0x61736C63), de.kapsi.net.daap.chunks.impl.SongLongDescription.class); //aslc
        map.put(new Integer(0x61737276), de.kapsi.net.daap.chunks.impl.SongRelativeVolume.class); //asrv
        map.put(new Integer(0x61737372), de.kapsi.net.daap.chunks.impl.SongSampleRate.class); //assr
        map.put(new Integer(0x6173737A), de.kapsi.net.daap.chunks.impl.SongSize.class); //assz
        map.put(new Integer(0x61737374), de.kapsi.net.daap.chunks.impl.SongStartTime.class); //asst
        map.put(new Integer(0x61737370), de.kapsi.net.daap.chunks.impl.SongStopTime.class); //assp
        map.put(new Integer(0x6173746D), de.kapsi.net.daap.chunks.impl.SongTime.class); //astm
        map.put(new Integer(0x61737463), de.kapsi.net.daap.chunks.impl.SongTrackCount.class); //astc
        map.put(new Integer(0x6173746E), de.kapsi.net.daap.chunks.impl.SongTrackNumber.class); //astn
        map.put(new Integer(0x61737572), de.kapsi.net.daap.chunks.impl.SongUserRating.class); //asur
        map.put(new Integer(0x61737972), de.kapsi.net.daap.chunks.impl.SongYear.class); //asyr
        map.put(new Integer(0x6D74636F), de.kapsi.net.daap.chunks.impl.SpecifiedTotalCount.class); //mtco
        map.put(new Integer(0x6D737474), de.kapsi.net.daap.chunks.impl.Status.class); //mstt
        map.put(new Integer(0x6D737473), de.kapsi.net.daap.chunks.impl.StatusString.class); //msts
        map.put(new Integer(0x6D73616C), de.kapsi.net.daap.chunks.impl.SupportsAutoLogout.class); //msal
        map.put(new Integer(0x6D736272), de.kapsi.net.daap.chunks.impl.SupportsBrowse.class); //msbr
        map.put(new Integer(0x6D736578), de.kapsi.net.daap.chunks.impl.SupportsExtensions.class); //msex
        map.put(new Integer(0x6D736978), de.kapsi.net.daap.chunks.impl.SupportsIndex.class); //msix
        map.put(new Integer(0x6D737069), de.kapsi.net.daap.chunks.impl.SupportsPersistentIds.class); //mspi
        map.put(new Integer(0x6D737179), de.kapsi.net.daap.chunks.impl.SupportsQuery.class); //msqy
        map.put(new Integer(0x6D737273), de.kapsi.net.daap.chunks.impl.SupportsResolve.class); //msrs
        map.put(new Integer(0x6D737570), de.kapsi.net.daap.chunks.impl.SupportsUpdate.class); //msup
        map.put(new Integer(0x6D73746D), de.kapsi.net.daap.chunks.impl.TimeoutInterval.class); //mstm
        map.put(new Integer(0x6D757064), de.kapsi.net.daap.chunks.impl.UpdateResponse.class); //mupd
        map.put(new Integer(0x6D757479), de.kapsi.net.daap.chunks.impl.UpdateType.class); //muty
    }

    public Class<? extends Chunk> getChunkClass(Integer contentCode) {
        return map.get(contentCode);
    }

    public Chunk newChunk(int contentCode) {
        Class<? extends Chunk> clazz = getChunkClass(new Integer(contentCode));
        try {
            return clazz.newInstance();
        } catch (Exception err) {
            throw new RuntimeException(DaapUtil.toContentCodeString(contentCode), err);
        }
    }
}
