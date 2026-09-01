package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.adventure.AdventureParser
import net.sourceforge.kolmafia.adventure.AdventureRequest
import net.sourceforge.kolmafia.adventure.MacroStrategy
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.adventure.choice.OutfitPool
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.adventure.runHedgeMaze
import net.sourceforge.kolmafia.adventure.runTowerDoor
import net.sourceforge.kolmafia.adventure.TowerDoorConfig
import net.sourceforge.kolmafia.adventure.TowerDoorStatus
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.DescriptionCache
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.buffbot.BuffBotDatabase
import net.sourceforge.kolmafia.buffbot.BuffBotManager
import net.sourceforge.kolmafia.faxbot.FaxBotDatabase
import net.sourceforge.kolmafia.faxbot.FaxBotManager
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.combat.RandomModifierStats
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.campground.CampgroundSync
import net.sourceforge.kolmafia.campground.GardenSync
import net.sourceforge.kolmafia.campground.MushroomManager
import net.sourceforge.kolmafia.campground.MushroomPlotSync
import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.concoction.StillSync
import net.sourceforge.kolmafia.inventory.ClosetMeatSync
import net.sourceforge.kolmafia.inventory.SessionMeatSync
import net.sourceforge.kolmafia.inventory.StorageMeatSync
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.CharacterStatusRefresh
import net.sourceforge.kolmafia.character.ClassResourceCharpaneSync
import net.sourceforge.kolmafia.character.CharpaneStatusSync
import net.sourceforge.kolmafia.character.ApiStatusSync
import net.sourceforge.kolmafia.clan.ClanIdSync
import net.sourceforge.kolmafia.character.ClassResourceCombatSync
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.FamiliarSoupSync
import net.sourceforge.kolmafia.character.FamTeamSync
import net.sourceforge.kolmafia.character.FightPokefamSync
import net.sourceforge.kolmafia.character.PokefamBoostSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.equipment.ModeableState
import net.sourceforge.kolmafia.equipment.OutfitCheckpoint
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.request.CraftRequest
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarRequest
import net.sourceforge.kolmafia.familiar.FamiliarSync
import net.sourceforge.kolmafia.inventory.AccessCountContext
import net.sourceforge.kolmafia.inventory.AccessibleItemCount
import net.sourceforge.kolmafia.inventory.CollectionCacheSync
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.modifiers.StatNames
import net.sourceforge.kolmafia.location.LocationDatabase
import net.sourceforge.kolmafia.mood.EditMoodCommandParser
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.mood.maximalSet
import net.sourceforge.kolmafia.mood.minimalSet
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.BeachCombChoiceSync
import net.sourceforge.kolmafia.quest.FloristFriarChoiceSync
import net.sourceforge.kolmafia.quest.SpacegateLeftoversChoiceSync
import net.sourceforge.kolmafia.quest.WlfBunkerChoiceSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestManager
import net.sourceforge.kolmafia.recovery.RecoveryManager
import net.sourceforge.kolmafia.request.AlliedRadioRequest
import net.sourceforge.kolmafia.request.AutosellRequest
import net.sourceforge.kolmafia.request.BatFellowRequest
import net.sourceforge.kolmafia.request.PulverizeRequest
import net.sourceforge.kolmafia.request.QuantumTerrariumRequest
import net.sourceforge.kolmafia.request.SpelunkyRequest
import net.sourceforge.kolmafia.request.ZapRequest
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.CafePurchaseRequest
import net.sourceforge.kolmafia.request.ChewRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import net.sourceforge.kolmafia.request.ClanHallRequest
import net.sourceforge.kolmafia.request.ClanMembersRequest
import net.sourceforge.kolmafia.request.ClanLogRequest
import net.sourceforge.kolmafia.request.ClanWarRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DrinkBoozeRequest
import net.sourceforge.kolmafia.request.EatFoodRequest
import net.sourceforge.kolmafia.request.StillSuitRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.CreateItemCraftSync
import net.sourceforge.kolmafia.shop.CoinmasterManager
import net.sourceforge.kolmafia.shop.NpcShopSync
import net.sourceforge.kolmafia.shop.ShopInventorySync
import net.sourceforge.kolmafia.shop.SwaggerShopSync
import net.sourceforge.kolmafia.shop.SeptEmberSync
import net.sourceforge.kolmafia.quest.AirportSync
import net.sourceforge.kolmafia.quest.GingerbreadCitySync
import net.sourceforge.kolmafia.quest.SpacegateVisitSync
import net.sourceforge.kolmafia.quest.SpacegateTerminalSync
import net.sourceforge.kolmafia.quest.SpacegateAdventureSync
import net.sourceforge.kolmafia.shop.TimeTowerSync
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.maximizer.MaximizerManager
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.request.ThriftyRequest
import net.sourceforge.kolmafia.request.TrendyRequest
import net.sourceforge.kolmafia.request.UneffectAction
import net.sourceforge.kolmafia.request.UneffectActionContext
import net.sourceforge.kolmafia.request.UneffectActionResolver
import net.sourceforge.kolmafia.request.UneffectItemAcquisition
import net.sourceforge.kolmafia.request.UneffectRemovableMaps
import net.sourceforge.kolmafia.request.UntinkerRequest
import net.sourceforge.kolmafia.request.ItemUseLimitsContext
import net.sourceforge.kolmafia.request.ModeableRequest
import net.sourceforge.kolmafia.request.HorseryRequest
import net.sourceforge.kolmafia.request.BoomBoxRequest
import net.sourceforge.kolmafia.request.MindControlRequest
import net.sourceforge.kolmafia.request.AbsorbRequest
import net.sourceforge.kolmafia.request.ManageStoreRequest
import net.sourceforge.kolmafia.quest.PirateRealmSync
import net.sourceforge.kolmafia.quest.DispensarySync
import net.sourceforge.kolmafia.quest.IslandWarActionResponseSync
import net.sourceforge.kolmafia.quest.IslandWarVisitLogSync
import net.sourceforge.kolmafia.quest.IslandWarVisitSync
import net.sourceforge.kolmafia.quest.PalindomeSync
import net.sourceforge.kolmafia.quest.PyramidVisitSync
import net.sourceforge.kolmafia.quest.DesertVisitSync
import net.sourceforge.kolmafia.quest.BlackForestSync
import net.sourceforge.kolmafia.quest.HiddenCityVisitSync
import net.sourceforge.kolmafia.quest.GarbageBeanstalkSync
import net.sourceforge.kolmafia.quest.ZeppelinRonSync
import net.sourceforge.kolmafia.quest.WhiteCitadelSync
import net.sourceforge.kolmafia.quest.ClancyNcSync
import net.sourceforge.kolmafia.quest.TowerRuinsSync
import net.sourceforge.kolmafia.quest.ExtremeSlopeSync
import net.sourceforge.kolmafia.quest.PirateNcSync
import net.sourceforge.kolmafia.quest.FarmDuckSync
import net.sourceforge.kolmafia.quest.ElVibratoSync
import net.sourceforge.kolmafia.quest.FriarsQuestSync
import net.sourceforge.kolmafia.quest.FantasyRealmSync
import net.sourceforge.kolmafia.quest.SwampQuestSync
import net.sourceforge.kolmafia.quest.CyberRealmSync
import net.sourceforge.kolmafia.quest.TownUnlockSync
import net.sourceforge.kolmafia.quest.ToppingPlaceSync
import net.sourceforge.kolmafia.quest.BatholeSync
import net.sourceforge.kolmafia.quest.PlainsVisitSync
import net.sourceforge.kolmafia.quest.SeaVisitSync
import net.sourceforge.kolmafia.quest.TrapperCabinSync
import net.sourceforge.kolmafia.quest.SneakyPeteDiscardSync
import net.sourceforge.kolmafia.quest.TrickOrTreatSync
import net.sourceforge.kolmafia.quest.PandamoniumVisitSync
import net.sourceforge.kolmafia.quest.CouncilVisitSync
import net.sourceforge.kolmafia.quest.IslandUnlockSync
import net.sourceforge.kolmafia.quest.FernruinVisitSync
import net.sourceforge.kolmafia.quest.TavernVisitSync
import net.sourceforge.kolmafia.quest.TavernCellarSync
import net.sourceforge.kolmafia.quest.DetectiveCaseSync
import net.sourceforge.kolmafia.quest.ToppingPeakNcSync
import net.sourceforge.kolmafia.quest.ProtonicGhostSync
import net.sourceforge.kolmafia.quest.QuestFightStartedSync
import net.sourceforge.kolmafia.quest.QuestItemUsedSync
import net.sourceforge.kolmafia.quest.FantasyRealmCombatSync
import net.sourceforge.kolmafia.quest.MelvinShirtSync
import net.sourceforge.kolmafia.quest.Cell37EscapeSync
import net.sourceforge.kolmafia.quest.ShenSync
import net.sourceforge.kolmafia.request.PeeVPeeRequest
import net.sourceforge.kolmafia.request.PlaceSync
import net.sourceforge.kolmafia.request.DwarfFactoryRequest
import net.sourceforge.kolmafia.request.DwarfContraptionRequest
import net.sourceforge.kolmafia.request.ArcadeRequest
import net.sourceforge.kolmafia.request.BasementSync
import net.sourceforge.kolmafia.quest.ShadowRiftSync
import net.sourceforge.kolmafia.request.ProfileRequest
import net.sourceforge.kolmafia.request.PortalRequest
import net.sourceforge.kolmafia.request.ElvmachineRequest
import net.sourceforge.kolmafia.quest.HiddenCityChoiceSync
import net.sourceforge.kolmafia.quest.PartyFairChoiceSync
import net.sourceforge.kolmafia.quest.LightsOutChoiceSync
import net.sourceforge.kolmafia.quest.SnojoChoiceSync
import net.sourceforge.kolmafia.quest.SpoopyChoiceSync
import net.sourceforge.kolmafia.quest.MonorailChoiceSync
import net.sourceforge.kolmafia.quest.SpacegateVaccinatorChoiceSync
import net.sourceforge.kolmafia.quest.VillainLairChoiceSync
import net.sourceforge.kolmafia.quest.TrickOrTreatChoiceSync
import net.sourceforge.kolmafia.quest.ArchSpadeChoiceSync
import net.sourceforge.kolmafia.quest.DeckChoiceSync
import net.sourceforge.kolmafia.quest.AutomatedFutureChoiceSync
import net.sourceforge.kolmafia.quest.MobiusChoiceSync
import net.sourceforge.kolmafia.quest.BaseballChoiceSync
import net.sourceforge.kolmafia.quest.MushyCenterChoiceSync
import net.sourceforge.kolmafia.quest.HorseryChoiceSync
import net.sourceforge.kolmafia.quest.MimicDnaChoiceSync
import net.sourceforge.kolmafia.quest.StalagmiteChoiceSync
import net.sourceforge.kolmafia.quest.PowerPlantChoiceSync
import net.sourceforge.kolmafia.quest.ColdMedicineChoiceSync
import net.sourceforge.kolmafia.quest.PlumberShopChoiceSync
import net.sourceforge.kolmafia.quest.BackupCameraChoiceSync
import net.sourceforge.kolmafia.quest.CrystalBallChoiceSync
import net.sourceforge.kolmafia.quest.AutumnatonChoiceSync
import net.sourceforge.kolmafia.quest.TrainsetChoiceSync
import net.sourceforge.kolmafia.quest.JuneCleaverChoiceSync
import net.sourceforge.kolmafia.quest.BurningLeavesChoiceSync
import net.sourceforge.kolmafia.quest.YouRobotChoiceSync
import net.sourceforge.kolmafia.quest.MayamChoiceSync
import net.sourceforge.kolmafia.quest.TakerSpaceChoiceSync
import net.sourceforge.kolmafia.quest.SpecimenBenchChoiceSync
import net.sourceforge.kolmafia.quest.HaciendaChoiceSync
import net.sourceforge.kolmafia.quest.LeprecondoChoiceSync
import net.sourceforge.kolmafia.quest.PerilChoiceSync
import net.sourceforge.kolmafia.quest.PeridotChoiceSync
import net.sourceforge.kolmafia.quest.CrimboPastChoiceSync
import net.sourceforge.kolmafia.quest.CoolerYetiChoiceSync
import net.sourceforge.kolmafia.quest.CartographyChoiceSync
import net.sourceforge.kolmafia.quest.SausageGrinderChoiceSync
import net.sourceforge.kolmafia.quest.BoomBoxChoiceSync
import net.sourceforge.kolmafia.quest.RedSnapperChoiceSync
import net.sourceforge.kolmafia.quest.DoctorBagChoiceSync
import net.sourceforge.kolmafia.quest.VoteBallotChoiceSync
import net.sourceforge.kolmafia.quest.LatteChoiceSync
import net.sourceforge.kolmafia.quest.MotorbikeChoiceSync
import net.sourceforge.kolmafia.quest.GenieChoiceSync
import net.sourceforge.kolmafia.quest.ControlPanelChoiceSync
import net.sourceforge.kolmafia.quest.DartPerksChoiceSync
import net.sourceforge.kolmafia.quest.DaycareChoiceSync
import net.sourceforge.kolmafia.quest.GnasirChoiceSync
import net.sourceforge.kolmafia.quest.HashingChoiceSync
import net.sourceforge.kolmafia.quest.HybridizationChoiceSync
import net.sourceforge.kolmafia.quest.IceHouseChoiceSync
import net.sourceforge.kolmafia.quest.MonkeyPawChoiceSync
import net.sourceforge.kolmafia.quest.TeaTreeChoiceSync
import net.sourceforge.kolmafia.quest.QuestLogSync
import net.sourceforge.kolmafia.quest.SpookyravenManorVisitSync
import net.sourceforge.kolmafia.quest.SorceressLairSync
import net.sourceforge.kolmafia.quest.TelescopeSync
import net.sourceforge.kolmafia.quest.TowerSync
import net.sourceforge.kolmafia.request.QuestLogRequest
import net.sourceforge.kolmafia.request.SendGiftRequest
import net.sourceforge.kolmafia.request.SendMailRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.request.SushiConsumptionSync
import net.sourceforge.kolmafia.request.BarrelChoiceMapper
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.request.HashingViseRequest
import net.sourceforge.kolmafia.request.PottedTeaTreeRequest
import net.sourceforge.kolmafia.request.ForeseeRequest
import net.sourceforge.kolmafia.request.KgbRequest
import net.sourceforge.kolmafia.request.PizzaCubeRequest
import net.sourceforge.kolmafia.request.UseItemConsumptionSync
import net.sourceforge.kolmafia.adventure.choice.ChoiceUtilities
import net.sourceforge.kolmafia.session.BreakfastManager
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.request.AfterLifeRequest
import net.sourceforge.kolmafia.request.SpaaaceRequest
import net.sourceforge.kolmafia.session.GreyYouManager
import net.sourceforge.kolmafia.session.ValhallaManager
import net.sourceforge.kolmafia.session.GrimstoneManager
import net.sourceforge.kolmafia.session.YouRobotManager
import net.sourceforge.kolmafia.session.AdventureSpentTracker
import net.sourceforge.kolmafia.session.BastilleBattalionSync
import net.sourceforge.kolmafia.session.BarrelShrineSync
import net.sourceforge.kolmafia.session.GuildVisitSync
import net.sourceforge.kolmafia.session.BastilleSyncContext
import net.sourceforge.kolmafia.session.DreadKissesTracker
import net.sourceforge.kolmafia.session.DreadScrollManager
import net.sourceforge.kolmafia.session.MerkinQuestSync
import net.sourceforge.kolmafia.session.SeaMerkinSync
import net.sourceforge.kolmafia.session.VoteMonsterManager
import net.sourceforge.kolmafia.session.FightStructuralSync
import net.sourceforge.kolmafia.session.FightIotmResidualSync
import net.sourceforge.kolmafia.session.StillSuitManager
import net.sourceforge.kolmafia.session.CrystalBallManager
import net.sourceforge.kolmafia.session.LocketManager
import net.sourceforge.kolmafia.session.ChibiBuddyManager
import net.sourceforge.kolmafia.request.GourdRequest
import net.sourceforge.kolmafia.quest.FireExtinguisherCombatSync
import net.sourceforge.kolmafia.quest.FightItemPrefSync
import net.sourceforge.kolmafia.quest.NewYouCombatSync
import net.sourceforge.kolmafia.session.BugbearManager
import net.sourceforge.kolmafia.session.CryptManager
import net.sourceforge.kolmafia.session.ElVibratoManager
import net.sourceforge.kolmafia.session.DemonInCombatNameSync
import net.sourceforge.kolmafia.session.EventHistory
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.DvorakManager
import net.sourceforge.kolmafia.session.SpadingManager
import net.sourceforge.kolmafia.session.MailManager
import net.sourceforge.kolmafia.session.ContactManager
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.StoreManager
import net.sourceforge.kolmafia.session.ResponseTextParser
import net.sourceforge.kolmafia.session.LeafletManager
import net.sourceforge.kolmafia.session.RabbitHoleManager
import net.sourceforge.kolmafia.session.WumpusManager
import net.sourceforge.kolmafia.session.PeeVPeeSync
import net.sourceforge.kolmafia.session.DemonNamesManager
import net.sourceforge.kolmafia.session.AlliedRadioManager
import net.sourceforge.kolmafia.session.CargoCultManager
import net.sourceforge.kolmafia.session.CargoPocketSync
import net.sourceforge.kolmafia.session.SkillLearnFromResponse
import net.sourceforge.kolmafia.quest.DynamicItemModifierSync
import net.sourceforge.kolmafia.quest.BirdOfTheDaySync
import net.sourceforge.kolmafia.skill.SkillLearner
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillType
import net.sourceforge.kolmafia.quest.DescriptionConsequenceSync
import net.sourceforge.kolmafia.quest.CombatSkillConsequenceSync
import net.sourceforge.kolmafia.quest.EffectDescriptionConsequenceSync
import net.sourceforge.kolmafia.quest.SkillGrantingEquipmentSync
import net.sourceforge.kolmafia.quest.SkillDescriptionConsequenceSync
import net.sourceforge.kolmafia.quest.ItemDescriptionConsequenceSync
import net.sourceforge.kolmafia.quest.CrownBjornDescSync
import net.sourceforge.kolmafia.quest.Crimbo23ZoneSync
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.session.SummoningChamberManager
import net.sourceforge.kolmafia.session.WildfireCampManager
import net.sourceforge.kolmafia.session.YegDemonNameSync
import net.sourceforge.kolmafia.session.PastaThrall
import net.sourceforge.kolmafia.request.NemesisRequest
import net.sourceforge.kolmafia.request.TavernRequest
import net.sourceforge.kolmafia.request.ActionBarRequest
import net.sourceforge.kolmafia.request.LocketRequest
import net.sourceforge.kolmafia.chat.ChatProbe
import net.sourceforge.kolmafia.chat.ChatSender
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GameRuntimeLibrary(
    internal val character: KoLCharacter? = null,
    internal val inventoryManager: InventoryManager? = null,
    internal val skillManager: SkillManager? = null,
    internal val effectManager: EffectManager? = null,
    internal val adventureManager: AdventureManager? = null,
    // new params — all nullable so forTesting() and existing tests still compile
    internal val familiarManager: FamiliarManager? = null,
    internal val goalManager: GoalManager? = null,
    internal val moodManager: MoodManager? = null,
    internal val manaBurnManager: net.sourceforge.kolmafia.mood.ManaBurnManager? = null,
    internal val preferences: Preferences? = null,
    internal val gameDatabase: GameDatabase? = null,
    internal val useItemRequest: UseItemRequest? = null,
    internal val eatFoodRequest: EatFoodRequest? = null,
    internal val drinkBoozeRequest: DrinkBoozeRequest? = null,
    internal val chewRequest: ChewRequest? = null,
    internal val cafePurchaseRequest: CafePurchaseRequest? = null,
    internal val stillSuitRequest: StillSuitRequest? = null,
    internal val actionBarRequest: ActionBarRequest? = null,
    internal val autosellRequest: AutosellRequest? = null,
    internal val pulverizeRequest: PulverizeRequest? = null,
    internal val zapRequest: ZapRequest? = null,
    internal val untinkerRequest: net.sourceforge.kolmafia.request.UntinkerRequest? = null,
    internal val closetRequest: ClosetRequest? = null,
    internal val storageRequest: StorageRequest? = null,
    internal val banishManager: BanishManager? = null,
    internal val httpClient: HttpClient? = null,
    internal val hermitRequest: HermitRequest? = null,
    internal val thriftyRequest: ThriftyRequest? = null,
    internal val standardRequest: StandardRequest? = null,
    internal val trendyRequest: TrendyRequest? = null,
    internal val displayCaseRequest: DisplayCaseRequest? = null,
    internal val clanStashRequest: ClanStashRequest? = null,
    internal val mallManager: MallManager? = null,
    internal val retrieveItemService: RetrieveItemService? = null,
    internal val outfitManager: OutfitManager? = null,
    internal val equipmentRequest: EquipmentRequest? = null,
    internal val equipmentManager: net.sourceforge.kolmafia.session.EquipmentManager? = null,
    internal val coinmasterManager: CoinmasterManager? = null,
    internal val craftRequest: CraftRequest? = null,
    internal val manageStoreRequest: ManageStoreRequest? = null,
    internal val mallPriceManager: MallPriceManager? = null,
    internal val characterRequest: CharacterRequest? = null,
    internal val recoveryManager: RecoveryManager? = null,
    internal val adventureRequest: AdventureRequest? = null,
    internal val uneffectRequest: net.sourceforge.kolmafia.request.UneffectRequest? = null,
    internal val questDatabase: QuestDatabase? = null,
    internal val questLogRequest: QuestLogRequest? = null,
    internal val clanLoungeRequest: ClanLoungeRequest? = null,
    internal val familiarRequest: FamiliarRequest? = null,
    internal val chatSender: ChatSender? = null,
    internal val maximizerManager: MaximizerManager? = null,
    internal val sessionLogger: net.sourceforge.kolmafia.session.SessionLogger? = null,
    internal val eventBus: GameEventBus? = null,
    internal val breakfastManager: BreakfastManager? = null,
    internal val sendMailRequest: SendMailRequest? = null,
    internal val sendGiftRequest: SendGiftRequest? = null,
    internal val choiceRequest: ChoiceRequest? = null,
    internal val hashingViseRequest: HashingViseRequest? = null,
    internal val pottedTeaTreeRequest: PottedTeaTreeRequest? = null,
    internal val foreseeRequest: ForeseeRequest? = null,
    internal val kgbRequest: KgbRequest? = null,
    internal val pizzaCubeRequest: PizzaCubeRequest? = null,
    internal val edServantManager: net.sourceforge.kolmafia.servant.EdServantManager? = null,
    internal val vykeaCompanionManager: net.sourceforge.kolmafia.vykea.VykeaCompanionManager? = null,
    internal val pastaThrallManager: net.sourceforge.kolmafia.thrall.PastaThrallManager? = null,
    internal val adventureSpentTracker: AdventureSpentTracker? = null,
    internal val dreadKissesTracker: DreadKissesTracker? = null,
    internal val wildfireCampManager: WildfireCampManager? = null,
    internal val summoningChamberManager: SummoningChamberManager? = null,
    internal val alliedRadioManager: AlliedRadioManager? = null,
    internal val cargoPocketSync: CargoPocketSync? = null,
    internal val cargoCultManager: CargoCultManager? = null,
    internal val yegDemonNameSync: YegDemonNameSync? = null,
    internal val demonInCombatNameSync: DemonInCombatNameSync? = null,
    internal val demonNamesManager: DemonNamesManager? = null,
    internal val cleanupJunkRunner: net.sourceforge.kolmafia.session.CleanupJunkRunner? = null,
    internal val autoMallRunner: net.sourceforge.kolmafia.session.AutoMallRunner? = null,
    internal val quarkRunner: net.sourceforge.kolmafia.session.QuarkRunner? = null,
    internal val buffBotManager: BuffBotManager? = null,
    internal val buffBotDatabase: BuffBotDatabase? = null,
    internal val faxBotManager: FaxBotManager? = null,
    internal val faxBotDatabase: FaxBotDatabase? = null,
    internal val chatProbe: ChatProbe? = null,
    internal val chatManager: net.sourceforge.kolmafia.chat.ChatManager? = null,
    internal val researchBenchRequest: net.sourceforge.kolmafia.request.ResearchBenchRequest? = null,
    internal val gourdRequest: GourdRequest? = null,
    internal val concoctionQueueRunner: net.sourceforge.kolmafia.session.ConcoctionQueueRunner? = null,
    internal val concoctionCreateRequest: net.sourceforge.kolmafia.request.ConcoctionCreateRequest? = null,
    internal val modeableRequest: ModeableRequest? = null,
    internal val horseryRequest: HorseryRequest? = null,
    internal val boomBoxRequest: BoomBoxRequest? = null,
    internal val mindControlRequest: MindControlRequest? = null,
    internal val absorbRequest: AbsorbRequest? = null,
    internal val numberologyRequest: net.sourceforge.kolmafia.request.NumberologyRequest? = null,
    internal val grandpaRequest: net.sourceforge.kolmafia.request.GrandpaRequest? = null,
    internal val shrineRequest: net.sourceforge.kolmafia.request.ShrineRequest? = null,
    internal val npcBuyRequest: net.sourceforge.kolmafia.npc.NpcBuyRequest? = null,
    internal val raffleRequest: net.sourceforge.kolmafia.request.RaffleRequest? = null,
    internal val sessionManager: net.sourceforge.kolmafia.session.SessionManager? = null,
) : RuntimeLibrary() {

    private val handledResidualResponseSignatures = mutableSetOf<Pair<String, String>>()

    private val moodCliContext = object : AshRuntimeContext {
        override fun print(msg: String) = Unit
        override fun lastCombatAction(): String = ""
        override fun setCombatAction(action: String) = Unit
    }

    init {
        preferences?.let { DynamicItemModifierSync.applyCachedOverrides(it) }
        moodManager?.cliExecutor = { cmd -> dispatchCli(cmd, moodCliContext) }
        manaBurnManager?.cliExecutor = { cmd -> dispatchCli(cmd, moodCliContext) }
        maximizerManager?.cliExecutor = { cmd ->
            dispatchCli(cmd, moodCliContext)
            true
        }
        maximizerManager?.progressDisplay = { msg ->
            chatManager?.notify(msg, "blue")
        }
        manaBurnManager?.accessibleCountProvider = { itemId ->
            val name = gameDatabase?.item(itemId)?.name
            if (name != null) physicalAccessibleCount(itemId, name) else 0
        }
        manaBurnManager?.manaCostAdjustmentProvider = {
            CombatAdjustment.manaCostModifier(buildCurrentModifiers(), combat = false)
        }
        manaBurnManager?.gameDatabase = gameDatabase
    }

    internal suspend fun refreshClosetCacheAfter(result: Result<*>?) {
        if (result?.isSuccess != true) return
        val prefs = preferences ?: return
        val request = closetRequest ?: return
        CollectionCacheSync.refreshCloset(request, prefs)
    }

    internal suspend fun refreshStorageCacheAfter(result: Result<*>?) {
        if (result?.isSuccess != true) return
        val prefs = preferences ?: return
        val request = storageRequest ?: return
        CollectionCacheSync.refreshStorage(request, character?.state?.value, prefs)
    }

    internal suspend fun refreshStashCacheAfter(result: Result<*>?) {
        if (result?.isSuccess != true) return
        val prefs = preferences ?: return
        val request = clanStashRequest ?: return
        CollectionCacheSync.refreshStash(request, prefs)
    }

    internal suspend fun refreshDisplayCacheAfter(result: Result<*>?) {
        if (result?.isSuccess != true) return
        val prefs = preferences ?: return
        val request = displayCaseRequest ?: return
        CollectionCacheSync.refreshDisplay(request, prefs)
    }

    companion object {
        /** Used in tests where no game managers are needed. */
        fun forTesting() = GameRuntimeLibrary()

        const val VERSION = "1.0.0-mobile"
        const val REVISION = "phase3830"
        internal const val CLI_ALIASES_PREF = "cliAliases"
        internal var waitMillis: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) }
    }

    /** Captured stdout from the most recent [cli_execute] call. */
    internal val lastCliOutput = StringBuilder()

    /** Desktop KoLmafiaCLI.previousLine — last dispatched CLI line (not `repeat`). */
    internal var previousLine: String? = null

    /** Desktop KoLmafiaCLI.elseRuns — whether a following else/elseif should run. */
    internal var elseShouldRun: Boolean = false

    /** Desktop KoLmafiaCLI.elseValid — whether else/elseif is legal here. */
    internal var elseValid: Boolean = false

    fun resolveCombatMacro(zoneId: String): String {
        net.sourceforge.kolmafia.session.ChoiceCombatAshState.combatFilterOverride
            ?.takeIf { it.isNotBlank() }
            ?.let { return net.sourceforge.kolmafia.combat.Macrofier.macrofy(filterOverride = it) ?: it }
        evaluateCombatAction()?.takeIf { it.isNotBlank() }?.let { return it }
        val prefs = preferences ?: return MacroStrategy.SAFE_DEFAULT
        return MacroStrategy.forLocation(zoneId, prefs)
    }

    fun evaluateCombatAction(): String? {
        val entry = activeCombatScriptEntry() ?: return null
        val runtime = AshRuntime(this)
        if (!runSavedScript(entry.name, runtime)) return null
        return runtime.lastCombatAction().takeIf { it.isNotBlank() }
    }

    fun activeCombatScriptEntry(): ScriptEntry? {
        val json = preferences?.getString(ScriptManager.SCRIPTS_PREF_KEY, "[]") ?: return null
        val scripts = try {
            Json.decodeFromString<List<ScriptEntry>>(json)
        } catch (_: Exception) {
            return null
        }
        val named = preferences?.getString(Preferences.COMBAT_SCRIPT, "")?.takeIf { it.isNotBlank() }
        if (named != null) {
            scripts.find { it.name.equals(named, ignoreCase = true) }?.let { return it }
        }
        return scripts.firstOrNull { it.type == ScriptType.COMBAT }
    }

    internal fun assignCombatScript(name: String) {
        preferences?.setString("combatMacro", name)
        preferences?.setString(Preferences.COMBAT_SCRIPT, name)
        val prefs = preferences ?: return
        val json = prefs.getString(ScriptManager.SCRIPTS_PREF_KEY, "[]")
        val scripts = try {
            Json.decodeFromString<List<ScriptEntry>>(json)
        } catch (_: Exception) {
            return
        }
        if (scripts.none { it.name.equals(name, ignoreCase = true) }) return
        val updated = scripts.map { entry ->
            when {
                entry.name.equals(name, ignoreCase = true) -> entry.copy(type = ScriptType.COMBAT)
                entry.type == ScriptType.COMBAT -> entry.copy(type = ScriptType.NORMAL)
                else -> entry
            }
        }
        prefs.setString(ScriptManager.SCRIPTS_PREF_KEY, Json.encodeToString(updated))
    }

    private fun runMoodCheckpointed(multiplicity: Int) {
        moodManager?.let { mood ->
            kotlinx.coroutines.runBlocking {
                mood.checkpointedExecute(
                    effectState = effectManager?.state?.value ?: EffectState(),
                    skillState = skillManager?.state?.value ?: SkillState(),
                    charState = character?.state?.value ?: CharacterState(),
                    character = character,
                    equipmentRequest = equipmentRequest,
                    gameDatabase = gameDatabase,
                    multiplicity = multiplicity,
                )
            }
        }
    }

    private fun parseMoodCliNameAndMultiplicity(raw: String): Pair<String, Int> {
        var params = raw.trim()
        var multiplicity = 0
        val lastSpace = params.lastIndexOf(' ')
        if (lastSpace != -1) {
            val possible = params.substring(lastSpace + 1)
            if (possible.isNotEmpty() && possible.all { it.isDigit() }) {
                multiplicity = possible.toIntOrNull() ?: 0
                params = params.substring(0, lastSpace).trim()
            }
        }
        return params to multiplicity
    }

    private fun moodEffectState(): EffectState =
        effectManager?.state?.value ?: EffectState()

    private fun moodSkillState(): SkillState =
        skillManager?.state?.value ?: SkillState()

    private fun moodCharState(): CharacterState =
        character?.state?.value ?: CharacterState()

    private fun dispatchEditMood(parameters: String, rt: AshRuntimeContext) {
        val mood = moodManager ?: return
        when {
            parameters.isEmpty() || parameters.equals("list", ignoreCase = true) -> {
                mood.activeEditMoodLines().forEach { line -> rt.print(line) }
            }
            parameters.equals("clear", ignoreCase = true) -> {
                if (mood.clearAllActiveTriggers()) {
                    rt.print("Cleared mood.")
                }
            }
            parameters.equals("autofill", ignoreCase = true) -> {
                mood.maximalSet(moodEffectState(), moodSkillState(), moodCharState())
                mood.saveActiveMood()
                mood.saveMoodLibrary()
                mood.activeEditMoodLines().forEach { line -> rt.print(line) }
            }
            else -> {
                val parsed = EditMoodCommandParser.parseParameters(parameters)
                if (parsed == null) {
                    rt.print("Invalid command: editmood $parameters")
                    return
                }
                val (type, effectName, action) = parsed
                val trigger = mood.addActiveRemovalTrigger(type, effectName, action.orEmpty())
                if (trigger == null) {
                    rt.print("Invalid command: editmood $parameters")
                    return
                }
                mood.saveActiveMood()
                mood.saveMoodLibrary()
                rt.print("Set mood trigger: ${mood.formatRemovalTriggerLine(trigger)}")
            }
        }
    }

    private val cliDispatch: List<Pair<Regex, (MatchResult, AshRuntimeContext) -> Unit>> = listOf(

        // "save as mood" — desktop SaveAsMoodCommand → minimalSet + save
        Regex("^save as mood$", RegexOption.IGNORE_CASE) to { _, _ ->
            moodManager?.let { mood ->
                mood.minimalSet(moodEffectState())
                mood.saveActiveMood()
                mood.saveMoodLibrary()
            }
        },

        // "editmood ..." — desktop EditMoodCommand
        Regex("^editmood(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            dispatchEditMood(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt)
        },

        // "trigger ..." — desktop EditMoodCommand plural alias
        Regex("^trigger(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            dispatchEditMood(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt)
        },

        // "mood execute" — run missing triggers for active mood
        Regex("^mood\\s+execute$", RegexOption.IGNORE_CASE) to { _, rt ->
            if (recoveryManager?.isRecoveryActive == true || moodManager?.isExecuting() == true) {
                return@to
            }
            runMoodCheckpointed(multiplicity = 0)
            rt.print("Mood swing complete.")
        },

        // "mood repeat [<n>]" — desktop MoodCommand repeat
        Regex("^mood\\s+repeat(?:\\s+(\\d+))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            if (recoveryManager?.isRecoveryActive == true || moodManager?.isExecuting() == true) {
                return@to
            }
            val multiplicity = m.groupValues.getOrNull(1)?.toIntOrNull() ?: 0
            runMoodCheckpointed(multiplicity = multiplicity)
            rt.print("Mood swing complete.")
        },

        // "mood autofill" — desktop MoodManager.maximalSet()
        Regex("^mood\\s+autofill$", RegexOption.IGNORE_CASE) to { _, _ ->
            moodManager?.let { mood ->
                mood.maximalSet(
                    effectState = effectManager?.state?.value ?: EffectState(),
                    skillState  = skillManager?.state?.value  ?: SkillState(),
                    charState   = character?.state?.value     ?: CharacterState(),
                )
                mood.saveActiveMood()
                mood.saveMoodLibrary()
            }
        },

        // bare "mood" or "mood list" — print active buff triggers
        Regex("^mood(?:\\s+list)?$", RegexOption.IGNORE_CASE) to { _, rt ->
            moodManager?.activeTriggerLines()?.forEach { line -> rt.print(line) }
        },

        // "mood listall" — print library mood names
        Regex("^mood\\s+listall$", RegexOption.IGNORE_CASE) to { _, rt ->
            moodManager?.libraryDisplayNames()?.forEach { name -> rt.print(name) }
        },

        // "mood clear" — remove all buff triggers from active mood
        Regex("^mood\\s+clear$", RegexOption.IGNORE_CASE) to { _, rt ->
            if (moodManager?.clearActiveTriggers() == true) {
                rt.print("Cleared mood.")
            }
        },

        // "mood <name> [<n>]" — set active mood; optional repeat-then-restore
        Regex("^mood\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val (name, multiplicity) = parseMoodCliNameAndMultiplicity(m.groupValues[1])
            val previousName = preferences?.getString(Preferences.ACTIVE_MOOD_NAME, "").orEmpty()
            if (moodManager?.setActiveMoodByName(name) != true) {
                return@to
            }
            if (multiplicity > 0) {
                if (recoveryManager?.isRecoveryActive == true || moodManager?.isExecuting() == true) {
                    return@to
                }
                runMoodCheckpointed(multiplicity = multiplicity)
                rt.print("Mood swing complete.")
                if (previousName.isNotBlank()) {
                    moodManager?.setActiveMoodByName(previousName)
                } else {
                    moodManager?.activeMood = null
                    moodManager?.saveActiveMood()
                }
            }
        },

        // "set key=value" — write a preference string
        Regex("^set\\s+(.+?)\\s*=\\s*(.*)$") to { m, _ ->
            preferences?.setString(m.groupValues[1].trim(), m.groupValues[2])
        },

        // "get key" — read and print a preference string
        Regex("^get\\s+(.+)$") to { m, rt ->
            val value = preferences?.getString(m.groupValues[1].trim(), "") ?: ""
            rt.print(value)
        },

        // bare cast lists castable skills; bare skill lists all (desktop UseSkillCommand)
        Regex("^(cast|skill)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val filter = if (m.groupValues[1].equals("cast", ignoreCase = true)) "cast" else ""
            cliSkills(filter, rt)
        },

        // "cast|skill N skill-name [^ effect]" — count form: silent no-op if unknown
        Regex("^(?:cast|skill)\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val parameters = "${m.groupValues[1]} ${m.groupValues[2].trim()}"
            cliCast(parameters, rt::print, echoUnknown = false)
        },

        // "cast|skill skill-name [^ effect]" — bare form: echo if unknown
        Regex("^(?:cast|skill)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliCast(m.groupValues[1].trim(), rt::print, echoUnknown = true)
        },

        // familiar / familiar list [filter] — desktop FamiliarCommand listing
        Regex("^familiar$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliFamiliars("", rt)
        },
        Regex("^familiar\\s+list(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliFamiliars(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        // familiar lock|unlock — AshP903 / familiar.php?action=lockequip
        Regex("^familiar\\s+lock$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliFamiliarEquipmentLock(true, rt)
        },
        Regex("^familiar\\s+unlock$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliFamiliarEquipmentLock(false, rt)
        },

        // "familiar name" — switch to a familiar by species name (none/unequip already handled)
        Regex("^familiar\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val name = m.groupValues[1].trim()
            if (name.equals("none", ignoreCase = true) ||
                name.equals("unequip", ignoreCase = true)
            ) {
                kotlinx.coroutines.runBlocking { familiarManager?.setFamiliar(name) }
            } else {
                kotlinx.coroutines.runBlocking {
                    resolveUsableFamiliarRace(name)?.let { familiarManager?.setFamiliar(it.race) }
                }
            }
        },

        // "enthrone name" / "enthrone none" — Crown of Thrones
        Regex("^enthrone\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val name = m.groupValues[1].trim()
            if (name.isEmpty() || name.equals("none", ignoreCase = true) ||
                name.equals("unequip", ignoreCase = true)
            ) {
                kotlinx.coroutines.runBlocking { familiarManager?.setEnthroned(name) }
            } else {
                kotlinx.coroutines.runBlocking {
                    resolveUsableFamiliarRace(name)?.let { familiarManager?.setEnthroned(it.race) }
                }
            }
        },

        // "bjornify name" / "bjornify none" — Buddy Bjorn
        Regex("^bjornify\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val name = m.groupValues[1].trim()
            if (name.isEmpty() || name.equals("none", ignoreCase = true) ||
                name.equals("unequip", ignoreCase = true)
            ) {
                kotlinx.coroutines.runBlocking { familiarManager?.setBjornified(name) }
            } else {
                kotlinx.coroutines.runBlocking {
                    resolveUsableFamiliarRace(name)?.let { familiarManager?.setBjornified(it.race) }
                }
            }
        },

        // "horsery [horse]" — status or ride a horsery horse
        Regex("^horsery(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            val horse = m.groupValues.getOrNull(1)?.trim().orEmpty()
            if (horse.isEmpty()) {
                val current = preferences?.getString("_horsery", "").orEmpty()
                if (current.isBlank()) {
                    rt.print("No horsery horse currently selected.")
                } else {
                    rt.print("Current horsery horse: $current")
                }
                rt.print("Usage: horsery <horse>")
                return@to
            }
            kotlinx.coroutines.runBlocking {
                horseryRequest?.ride(horse)?.onFailure { rt.print(it.message ?: "horsery failed") }
            }
        },

        // "boombox [song]" — status or play a SongBoom track
        Regex("^boombox(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            val song = m.groupValues.getOrNull(1)?.trim().orEmpty()
            if (song.isEmpty()) {
                val current = preferences?.getString("boomBoxSong", "").orEmpty()
                if (current.isBlank()) {
                    rt.print("Boombox is currently off.")
                } else {
                    rt.print("Current boombox song: $current")
                }
                rt.print("Usage: boombox <song>")
                return@to
            }
            kotlinx.coroutines.runBlocking {
                boomBoxRequest?.play(song)?.onFailure { rt.print(it.message ?: "boombox failed") }
            }
        },

        // "latte unlocks|unlocked|refill a b c" — Latte Lovers Member's Mug
        Regex("^latte(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliLatte(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },

        // Phases 1011–1022 IoTM facility CLIs
        Regex("^(?:autumnaton|fallguy)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliAutumnaton(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^cmc(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliCmc(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^leaves(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliLeaves(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^teatree(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliTeatree(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^foresee(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliForesee(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^mummery(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliMummery(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^timespinner(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliTimespinner(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^florist(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliFlorist(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^leprecondo(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliLeprecondo(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^heist(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliHeist(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },

        // Phases 1033–1042 quest-complete / basement / nemesis / Lights Out / Tales / field CLIs
        Regex("^tavern(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliTavern(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^baron(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliBaron(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^gourd(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliGourd(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^pingpong(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliPingPong(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^ping(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runPingCli(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^dvorak(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliDvorak(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^sven(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliSven(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^basement(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliBasement(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^nemesis(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliNemesis(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^spookyraven(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliSpookyraven(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^taleofdread(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliTaleOfDread(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },
        Regex("^field(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliField(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },

        // "mcd|mind-control [level]" — status or set mind control device level
        Regex("^(?:mcd|mind-control)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            val arg = m.groupValues.getOrNull(1)?.trim().orEmpty()
            if (arg.isEmpty()) {
                val level = character?.state?.value?.mindControlLevel ?: 0
                rt.print("Mind control device level: $level")
                rt.print("Usage: mcd <level>")
                return@to
            }
            val level = arg.toIntOrNull() ?: run {
                rt.print("Usage: mcd <level>")
                return@to
            }
            kotlinx.coroutines.runBlocking {
                mindControlRequest?.setLevel(level)?.onFailure { rt.print(it.message ?: "mcd failed") }
            }
        },

        // "servant type" — Ed entombed servant switch
        Regex("^servant\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val type = m.groupValues[1].trim()
            kotlinx.coroutines.runBlocking {
                edServantManager?.useServant(type) { message -> rt.print(message) }
            }
        },

        // "servants" — list summoned Ed servants
        Regex("^servants$", RegexOption.IGNORE_CASE) to { _, rt ->
            edServantManager?.printStatus { message -> rt.print(message) }
                ?: rt.print("Only Ed the Undying has entombed servants!")
        },

        // "retrieve N item" / acquire / find — compound retrieve (qty optional, comma lists)
        Regex("^(?:acquire|find|retrieve)\\?\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runAcquireCli(m.groupValues[1].trim(), rt, checkOnly = true)
        },
        Regex("^(?:retrieve|acquire|find)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runAcquireCli(m.groupValues[1].trim(), rt)
        },

        // "use[?] N item" / "use item" — bang potion / slime resolution; ? = check-only
        Regex("^use(\\?)?(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliUse(
                m.groupValues.getOrNull(2)?.trim().orEmpty(),
                rt::print,
                checkOnly = m.groupValues.getOrNull(1) == "?",
            )
        },

        // "eat[?] N item" / "eat item" — VIP hot dogs via lounge; else inventory; ? = check-only
        Regex("^eat(\\?)?(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliEat(
                m.groupValues.getOrNull(2)?.trim().orEmpty(),
                rt::print,
                checkOnly = m.groupValues.getOrNull(1) == "?",
            )
        },

        // "drink[?] N item" / "drink item" — VIP speakeasy via lounge; else inventory; ? = check-only
        Regex("^drink(\\?)?(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliDrink(
                m.groupValues.getOrNull(2)?.trim().orEmpty(),
                rt::print,
                checkOnly = m.groupValues.getOrNull(1) == "?",
            )
        },

        // "chew[?] N item" / "chew item" — bang potion / slime resolution; ? = check-only
        Regex("^chew(\\?)?(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliChew(
                m.groupValues.getOrNull(2)?.trim().orEmpty(),
                rt::print,
                checkOnly = m.groupValues.getOrNull(1) == "?",
            )
        },

        // "ghost N item" / "hobo N item" / "slimeling N item" / "robo item"
        Regex("^ghost\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking {
                familiarFeedItem(itemId, qty, net.sourceforge.kolmafia.data.ConcoctionConsumptionType.GLUTTONOUS_GHOST)
            }
        },
        Regex("^ghost\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val itemId = gameDatabase?.item(m.groupValues[1].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking {
                familiarFeedItem(itemId, 1, net.sourceforge.kolmafia.data.ConcoctionConsumptionType.GLUTTONOUS_GHOST)
            }
        },
        Regex("^hobo\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking {
                familiarFeedItem(itemId, qty, net.sourceforge.kolmafia.data.ConcoctionConsumptionType.SPIRIT_HOBO)
            }
        },
        Regex("^hobo\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val itemId = gameDatabase?.item(m.groupValues[1].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking {
                familiarFeedItem(itemId, 1, net.sourceforge.kolmafia.data.ConcoctionConsumptionType.SPIRIT_HOBO)
            }
        },
        Regex("^slimeling\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking {
                familiarFeedItem(itemId, qty, net.sourceforge.kolmafia.data.ConcoctionConsumptionType.SLIMELING)
            }
        },
        Regex("^slimeling\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val itemId = gameDatabase?.item(m.groupValues[1].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking {
                familiarFeedItem(itemId, 1, net.sourceforge.kolmafia.data.ConcoctionConsumptionType.SLIMELING)
            }
        },
        Regex("^robo\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val itemId = gameDatabase?.item(m.groupValues[1].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking {
                familiarFeedItem(itemId, 1, net.sourceforge.kolmafia.data.ConcoctionConsumptionType.ROBORTENDER)
            }
        },

        // craft queue drain — desktop UseItemDequeuePanel headless equivalents
        Regex("^eatqueue$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking {
                drainQueue(
                    net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket.FOOD,
                    net.sourceforge.kolmafia.data.ConcoctionConsumptionType.EAT,
                )
            }
        },
        Regex("^drinkqueue$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking {
                drainQueue(
                    net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket.BOOZE,
                    net.sourceforge.kolmafia.data.ConcoctionConsumptionType.DRINK,
                )
            }
        },
        Regex("^chewqueue$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking {
                drainQueue(
                    net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket.SPLEEN,
                    net.sourceforge.kolmafia.data.ConcoctionConsumptionType.SPLEEN,
                )
            }
        },
        Regex("^usequeue$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking {
                drainQueue(
                    net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket.POTION,
                    net.sourceforge.kolmafia.data.ConcoctionConsumptionType.USE,
                )
            }
        },
        Regex("^ghostqueue$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking {
                drainQueue(
                    net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket.FOOD,
                    net.sourceforge.kolmafia.data.ConcoctionConsumptionType.GLUTTONOUS_GHOST,
                )
            }
        },
        Regex("^hoboqueue$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking {
                drainQueue(
                    net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket.BOOZE,
                    net.sourceforge.kolmafia.data.ConcoctionConsumptionType.SPIRIT_HOBO,
                )
            }
        },
        Regex("^slimelingqueue$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking {
                drainQueue(
                    net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket.FOOD,
                    net.sourceforge.kolmafia.data.ConcoctionConsumptionType.SLIMELING,
                )
            }
        },
        Regex("^roboequeue$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking {
                drainQueue(
                    net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket.BOOZE,
                    net.sourceforge.kolmafia.data.ConcoctionConsumptionType.ROBORTENDER,
                )
            }
        },
        Regex("^createqueue$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking { drainCreateQueues() }
        },

        // "eatsilent [N] item" / "drinksilent [N] item" — qty optional (desktop UseItemCommand)
        Regex("^eatsilent(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliEat(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },
        Regex("^drinksilent(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliDrink(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        // "visit <coinmaster>" — open coinmaster shop page
        Regex("^visit\\s+(\\S+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val master = coinmasterManager?.resolveMaster(m.groupValues[1].trim()) ?: return@to
            kotlinx.coroutines.runBlocking { coinmasterManager?.visit(master) }
        },

        // closet / display / stash put & take
        Regex("^closet\\s+(put|take)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            runClosetMoveCli(m.groupValues[1], m.groupValues[2])
        },
        Regex("^closet(?:\\s+list(?:\\s+(.*))?)?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliCloset(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^storage\\s+(put|take)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            runStorageMoveCli(m.groupValues[1], m.groupValues[2])
        },
        Regex("^storage(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliStorage(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^display\\s+(put|take)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            runDisplayMoveCli(m.groupValues[1], m.groupValues[2])
        },
        Regex("^display(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliDisplay(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^stash\\s+(put|take)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            runStashCli(m.groupValues[1], m.groupValues[2])
        },

        // goal add id:N — before generic goal add (order matters in cliDispatch)
        Regex("^goal\\s+add\\s+id:(\\d+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            goalManager?.addItemGoal(m.groupValues[1].toIntOrNull() ?: return@to)
        },

        // goal add/remove/clear
        Regex("^goal\\s+add\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            goalManager?.addItemGoalByName(m.groupValues[1].trim())
        },
        Regex("^goal\\s+remove\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            goalManager?.removeGoal(m.groupValues[1].trim())
        },
        Regex("^goal\\s+clear$", RegexOption.IGNORE_CASE) to { _, _ ->
            goalManager?.clearGoals()
        },

        Regex("^goal\\s+meat\\s+(\\d+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            goalManager?.setMeatGoal(m.groupValues[1].toIntOrNull() ?: return@to)
        },

        Regex("^goal\\s+level\\s+(\\d+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            goalManager?.setLevelGoal(m.groupValues[1].toIntOrNull() ?: return@to)
        },

        Regex("^goal\\s+choice\\s+(\\d+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            goalManager?.setChoiceGoal(m.groupValues[1].toIntOrNull() ?: return@to)
        },

        Regex("^goal\\s+substats$", RegexOption.IGNORE_CASE) to { _, _ ->
            goalManager?.setSubstatsGoal(true)
        },

        // set location zone — must precede generic set pref handler
        Regex("^set\\s+location\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val zoneName = m.groupValues[1].trim()
            val location = resolveLocation(zoneName) ?: return@to
            preferences?.setString(Preferences.LAST_LOCATION, location.name)
            kotlinx.coroutines.runBlocking {
                adventureRequest?.travel(location.id)
            }
        },

        // set pref value — bare preference alias
        Regex("^set\\s+(\\S+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            preferences?.setString(m.groupValues[1].trim(), m.groupValues[2])
        },

        // get pref value
        Regex("^get\\s+(\\S+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            rt.print(preferences?.getString(m.groupValues[1].trim(), "") ?: "")
        },

        // counter add/set — named integer counters
        Regex("^counter\\s+(\\S+)\\s+add\\s+(-?\\d+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val name = m.groupValues[1].trim()
            val delta = m.groupValues[2].toIntOrNull() ?: 0
            val prefs = preferences ?: return@to
            val key = "counter_$name"
            prefs.setInt(key, prefs.getInt(key, 0) + delta)
            prefs.registerCounterName(name)
        },
        Regex("^counter\\s+(\\S+)\\s+set\\s+(-?\\d+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val name = m.groupValues[1].trim()
            val value = m.groupValues[2].toIntOrNull() ?: 0
            preferences?.setInt("counter_$name", value)
            preferences?.registerCounterName(name)
        },

        // counter — print/set named pref, or list relay counters
        Regex("^counter\\s+relay$", RegexOption.IGNORE_CASE) to { _, rt ->
            val currentRun = character?.state?.value?.currentRun ?: 0
            val prefs = preferences ?: return@to
            val formatted = net.sourceforge.kolmafia.session.TurnCounter.formatRelayCounters(prefs, currentRun)
            if (formatted.isNotBlank()) rt.print(formatted)
        },
        Regex("^counters\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runCountersCli(m.groupValues[1].trim(), rt)
        },
        Regex("^counters$", RegexOption.IGNORE_CASE) to { _, rt ->
            val prefs = preferences ?: return@to
            for (name in prefs.counterNames()) {
                rt.print("$name: ${prefs.getInt("counter_$name", 0)}")
            }
        },
        Regex("^counter\\s+(\\S+)(?:\\s+(\\d+))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            val name = m.groupValues[1].trim()
            val value = m.groupValues.getOrNull(2)?.trim()
            if (value.isNullOrBlank()) {
                rt.print(preferences?.getInt("counter_$name", 0).toString())
            } else {
                preferences?.setInt("counter_$name", value.toIntOrNull() ?: 0)
                preferences?.registerCounterName(name)
            }
        },

        Regex("^choice-goal$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliChoiceGoal(rt::print)
        },

        Regex("^choice(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runChoiceCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },

        Regex("^thralls$", RegexOption.IGNORE_CASE) to { _, rt ->
            val prefs = preferences ?: return@to
            val table = PastaThrall.formatTable(prefs)
            if (table.isNotBlank()) rt.print(table)
        },

        Regex("^journey(?:\\s+(.+))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliJourney(m.groupValues[1], rt::print)
        },

        Regex("^witchess(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliWitchess(m.groupValues[1].trim(), rt::print)
        },

        Regex("^cemet(?:ery|ary)$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("place.php?whichplace=cemetery", applyQuestHooks = true)
        },

        Regex("^(?:clear|cls|reset)$", RegexOption.IGNORE_CASE) to { _, _ ->
            lastCliOutput.clear()
        },

        Regex("^enable\\s+(\\S+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            preferences?.setBoolean(m.groupValues[1].trim(), true)
        },
        Regex("^disable\\s+(\\S+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            preferences?.setBoolean(m.groupValues[1].trim(), false)
        },

        Regex("^volcano(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliVolcano(m.groupValues[1].trim(), rt::print)
        },

        Regex("^summon(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliSummon(m.groupValues[1].trim(), rt::print)
        },

        Regex("^wereprofessor(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliWereProfessor(m.groupValues[1].trim(), rt::print)
        },

        Regex("^demons(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliDemons(m.groupValues[1].trim(), rt::print)
        },

        Regex("^alliedradio(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliAlliedRadio(m.groupValues[1].trim(), rt::print)
        },

        Regex("^cargo(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliCargo(m.groupValues[1].trim(), rt::print)
        },

        Regex("^dreadscroll$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliDreadscroll(rt::print)
        },

        Regex("^barrelprayer(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliBarrelPrayer(m.groupValues[1].trim(), rt::print)
        },

        Regex("^concert(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliConcert(m.groupValues[1].trim(), rt::print)
        },

        Regex("^nuns(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliNuns(m.groupValues[1].trim(), rt::print)
        },

        Regex("^shower(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliShower(m.groupValues[1].trim(), rt::print)
        },

        Regex("^swim(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliSwim(m.groupValues[1].trim(), rt::print)
        },

        Regex("^ballpit$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliBallpit(rt::print)
        },

        Regex("^pillkeeper(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliPillkeeper(m.groupValues[1].trim(), rt::print)
        },

        Regex("^photobooth(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliPhotobooth(m.groupValues[1].trim(), rt::print)
        },

        Regex("^fortune(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliFortune(m.groupValues[1].trim(), rt::print)
        },

        Regex("^mom(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliMom(m.groupValues[1].trim(), rt::print)
        },

        Regex("^mayosoak$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliMayosoak(rt::print)
        },

        Regex("^genie(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliGenie(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^monkeypaw(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliMonkeypaw(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^monorail(?:\\s+.*)?$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliMonorail(rt::print)
        },

        Regex("^toggle(?:\\s+.*)?$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliToggle(rt::print)
        },

        Regex("^crossstreams(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliCrossstreams(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^styx(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliStyx(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^skeleton(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliSkeleton(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^(?:play|cheat)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliPlay(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^gong(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliGong(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^gap(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliGap(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^spacegate(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliSpacegate(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^daycare(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliDaycare(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^campground\\s+vault3$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliCampgroundVault3(rt::print)
        },
        Regex("^(?:campground|camp)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runCampgroundActionCli(m.groupValues[1].trim(), rt)
        },

        Regex("^grim(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliGrim(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^aprilband(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliAprilband(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^terminal(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliTerminal(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^campaway(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliCampaway(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^loathingidol(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliLoathingidol(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^mayam(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliMayam(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^asdonmartin(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliAsdonmartin(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^beach(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliBeach(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        // Phases 1053–1062 Oddball CLI Track E
        Regex("^skeeball(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliSkeeball(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },
        Regex("^vise(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliVise(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },
        Regex("^throw(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliThrow(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },
        Regex("^buffbot(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliBuffbot(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },
        Regex("^crimbotrain(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliCrimboTrain(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },
        Regex("^badmoon(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliBadMoon(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },
        Regex("^flicker(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliFlicker(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^skate(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliSkate(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^hatter(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliHatter(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },
        Regex("^chess(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliChess(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },
        Regex("^wumpus(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliWumpus(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },
        Regex("^leaflet(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliLeaflet(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },
        Regex("^bastille(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliBastille(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },
        Regex("^test\\s+bastille$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliBastille("test", rt::print)
        },
        Regex("^robot(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliRobot(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },
        Regex("^rumple(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliRumple(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^synthesize(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliSynthesize(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^factory$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("guild.php?place=paco", applyQuestHooks = true)
        },

        Regex("^dwarf(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliDwarfFactory(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },

        Regex("^factory\\s+(check|report|setdigits|solve|vacuum)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            val rest = listOfNotNull(
                m.groupValues.getOrNull(1),
                m.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() },
            ).joinToString(" ")
            cliDwarfFactory(rest, rt::print)
        },

        Regex("^(?:meatcar|knoll)$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("guild.php?place=paco", applyQuestHooks = true)
        },

        Regex("^(?:citadel|ocg)$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("guild.php?place=ocg", applyQuestHooks = true)
        },

        Regex("^unalias\\s+(\\S+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            if (removeCliAlias(m.groupValues[1].trim())) {
                rt.print("Alias removed.")
            } else {
                rt.print("No such alias.")
            }
        },

        Regex("^scg$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("guild.php?place=scg", applyQuestHooks = true)
        },

        Regex("^challenge$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("guild.php?place=challenge", applyQuestHooks = true)
        },

        Regex("^canadia$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("adventure.php?snarfblat=43")
        },

        Regex("^friars(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliFriars(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^desert$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("place.php?whichplace=desertbeach")
        },

        Regex("^woods$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("place.php?whichplace=woods", applyQuestHooks = true)
        },

        Regex("^mountains$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("place.php?whichplace=mountains", applyQuestHooks = true)
        },

        Regex("^beach$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("place.php?whichplace=desertbeach")
        },

        Regex("^pyramid$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("place.php?whichplace=desertbeach&action=db_pyramid1", applyQuestHooks = true)
        },

        // ccs / ccprep — combat macro text + optional saved COMBAT script
        Regex("^ccs$", RegexOption.IGNORE_CASE) to { _, rt ->
            runCcsStatusCli(rt)
        },
        Regex("^ccs\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            assignCombatScript(m.groupValues[1].trim())
        },
        Regex("^ccprep$", RegexOption.IGNORE_CASE) to { _, rt ->
            rt.print(preferences?.getString("combatMacro", "") ?: "")
        },

        // location shortcuts
        Regex("^spooky$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("adventure.php?snarfblat=61")
        },
        Regex("^cellar2?$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("cellar.php")
        },
        Regex("^tower(?:\\s+(needed))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliTowerDoorStatus(rt, m.groupValues[1].isNotBlank())
        },
        Regex("^lowkey(?:\\s+(needed))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliTowerDoorStatus(rt, m.groupValues[1].isNotBlank())
        },
        Regex("^fern$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("tower.php", applyQuestHooks = true)
        },
        Regex("^guild$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("guild.php", applyQuestHooks = true)
        },
        Regex("^guild\\s+(paco|ocg|scg|challenge)$", RegexOption.IGNORE_CASE) to { m, _ ->
            visitKolPage("guild.php?place=${m.groupValues[1].lowercase()}", applyQuestHooks = true)
        },

        // macro — print stored combat macro
        Regex("^macro$", RegexOption.IGNORE_CASE) to { _, rt ->
            rt.print(preferences?.getString("combatMacro", "") ?: "")
        },

        // jukebox <song> — clan rumpus jukebox (Maximizer / desktop JukeboxCommand)
        Regex("^jukebox(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliJukebox(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^(adv(?:enture)?)(\\??)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runAdventureCli(m.groupValues.getOrNull(3).orEmpty(), m.groupValues[2] == "?", rt)
        },
        Regex("^(numberology)(\\??)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runNumberologyCli(m.groupValues.getOrNull(3).orEmpty(), m.groupValues[2] == "?", rt)
        },

        // location — print last known location
        Regex("^location$", RegexOption.IGNORE_CASE) to { _, rt ->
            val loc = preferences?.getString(Preferences.LAST_LOCATION, "") ?: ""
            rt.print(loc)
        },

        // zone — print adventures.txt zone name for current location
        Regex("^zone$", RegexOption.IGNORE_CASE) to { _, rt ->
            val loc = preferences?.getString(Preferences.LAST_LOCATION, "") ?: ""
            val zone = AdventureDatabase.getByName(loc)?.zoneName ?: loc
            rt.print(zone)
        },

        // count [N] item — print inventory quantity (N ignored; desktop compatibility)
        Regex("^count\\s+(?:(\\d+)\\s+)?(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val itemName = m.groupValues[2].trim()
            val qty = inventoryManager?.state?.value?.items?.values
                ?.find { it.name.equals(itemName, ignoreCase = true) }?.quantity ?: 0
            rt.print(qty.toString())
        },

        // put_storage N item — alias for storage put
        Regex("^put_storage\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking {
                refreshStorageCacheAfter(storageRequest?.deposit(itemId, qty))
            }
        },

        // refresh [target] — desktop RefreshStatusCommand (bare refresh stays full sync)
        Regex("^refresh(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runRefreshCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },

        // questlog / quests — sync quest log pages
        Regex("^(?:questlog|quests|quest)$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking { questLogRequest?.syncAll() }
        },

        // quest NAME — print current step for one quest
        Regex("^quest\\s+(\\S+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val quest = resolveQuest(m.groupValues[1]) ?: run {
                rt.print(QuestDatabase.UNSTARTED)
                return@to
            }
            rt.print(questDatabase?.getProgress(quest) ?: QuestDatabase.UNSTARTED)
        },

        // whatis item — alias for description
        Regex("^whatis\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val item = gameDatabase?.item(m.groupValues[1].trim())
            if (item == null) {
                rt.print("Unknown item: ${m.groupValues[1].trim()}")
            } else {
                rt.print("${item.name} (${item.primaryUse.name.lowercase()}, autosell ${item.autosellPrice} meat)")
            }
        },

        // skills [filter] — fetch then list owned skills (desktop ShowDataCommand)
        Regex("^skills(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliSkills(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^(?:pass|passive)$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliSkills("passive", rt)
        },
        Regex("^self$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliSkills("self", rt)
        },
        Regex("^combat$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliSkills("combat", rt)
        },
        Regex("^effects(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliEffects(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^(?:inv|inventory)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliInventory(m.groupValues.getOrNull(1).orEmpty(), rt)
        },

        // contacts / mail — visit common KoL pages (Track D: mail list stub)
        Regex("^contacts$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("contacts.php")
        },
        Regex("^(?:mail|readmail)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliMail(m.groupValues.getOrNull(1).orEmpty(), rt)
        },

        // description / desc / show item — print item summary from database
        Regex("^show\\s+all$", RegexOption.IGNORE_CASE) to { _, rt ->
            rt.print(buildShowAllSummary())
        },
        Regex("^(?:description|desc|show)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val item = gameDatabase?.item(m.groupValues[1].trim())
            if (item != null) {
                rt.print("${item.name} [${item.primaryUse.name.lowercase()}] autosell=${item.autosellPrice}")
            }
        },

        // pool <stance>[,stance…] — VIP lounge billiards (Maximizer / desktop PoolCommand)
        Regex("^pool(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliPool(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        // hottub / soak — clan VIP lounge hot tub
        Regex("^(?:hottub|soak)$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking { clanLoungeRequest?.useHotTub(preferences) }
        },

        // profam <item> — use one copy (professional familiar leaflet pattern)
        Regex("^profam\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val itemName = m.groupValues[1].trim()
            val itemId = gameDatabase?.item(itemName)?.id ?: return@to
            kotlinx.coroutines.runBlocking { useItemRequest?.use(itemId, 1) }
        },

        // attack <target> stance= — directed PvP (desktop PvpAttackCommand)
        Regex("^attack(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliPvpAttack(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt)
        },

        // pvp attack — mobile alias; must precede generic pvp
        Regex("^pvp\\s+attack(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliPvpAttack(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt)
        },

        Regex("^pvp(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliPvp(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        Regex("^flowers$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliFlowers(rt::print)
        },

        Regex("^swagger$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliFlowers(rt::print)
        },

        // tags — list registered counter/mood tag names
        Regex("^tags$", RegexOption.IGNORE_CASE) to { _, rt ->
            val prefs = preferences ?: return@to
            for (name in prefs.counterNames()) {
                rt.print(name)
            }
            prefs.getString(Preferences.MOOD_LIBRARY_NAMES, "")
                .split('|')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .forEach { rt.print(it) }
        },

        // joke — harmless no-op for script compatibility
        Regex("^joke$", RegexOption.IGNORE_CASE) to { _, rt ->
            rt.print("That's funny.")
        },

        // refreshshop — compact alias (spaced "refresh shop" is handled above)
        Regex("^refreshshop$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking { manageStoreRequest?.refreshPrices() }
        },

        // itemnotify on/off — pref toggle (headless stub)
        Regex("^itemnotify\\s+(on|off)$", RegexOption.IGNORE_CASE) to { m, _ ->
            preferences?.setBoolean("itemNotify", m.groupValues[1].equals("on", ignoreCase = true))
        },

        // vendor / managestore / mall — visit store pages
        Regex("^(?:vendor|managestore)$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("managestore.php")
        },
        Regex("^mall$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("mallstore.php")
        },

        // familiars [filter] — desktop ShowDataCommand listing
        Regex("^familiars(?:\\s+(?:list\\s+)?(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliFamiliars(m.groupValues.getOrNull(1).orEmpty(), rt)
        },

        // steal — desktop PvpStealCommand alias; `steal N item` still familiar-steals (dual-route)
        Regex("^steal(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliSteal(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt)
        },

        // famsteal / familiarsteal — explicit familiar steal (same path as steal dual-route fallback)
        Regex("^(?:famsteal|familiarsteal)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliFamiliarStealAlias(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt)
        },

        // sendmsg channel message — public chat
        Regex("^sendmsg\\s+(\\S+)\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val channel = m.groupValues[1].trim()
            val message = m.groupValues[2]
            kotlinx.coroutines.runBlocking { chatSender?.send(channel, message) }
        },

        // msg player message — private chat
        Regex("^msg\\s+(\\S+)\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val recipient = m.groupValues[1].trim()
            val message = m.groupValues[2]
            kotlinx.coroutines.runBlocking { chatSender?.sendPrivate(recipient, message) }
        },

        // buff — desktop CommandAlias skills buff (fetch + list; must precede buff bot skill)
        Regex("^buff$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliSkills("buff", rt)
        },

        // buff bot skill [turns] — PM buffbot request protocol
        Regex("^buff\\s+(\\S+)\\s+(\\S+)(?:\\s+(\\d+))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            val bot = m.groupValues[1].trim()
            val skillToken = m.groupValues[2].trim()
            val turns = m.groupValues.getOrNull(3)?.trim()?.toIntOrNull()
            kotlinx.coroutines.runBlocking {
                runBuffRequestCli(bot, skillToken, turns, rt)
            }
        },

        // faxbot [command] — PM faxbot request protocol
        Regex("^faxbot\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val command = m.groupValues[1].trim()
            kotlinx.coroutines.runBlocking { runFaxbotCli(command, rt) }
        },

        // fax send|put|receive|get — clan VIP lounge fax machine
        Regex("^fax\\s+(send|put|receive|get)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val option = m.groupValues[1].trim()
            kotlinx.coroutines.runBlocking { runFaxCli(option, rt) }
        },

        // kmail recipient message — text-only kmail via sendmessage.php
        Regex("^kmail\\s+(\\S+)\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val recipient = m.groupValues[1].trim()
            val message = m.groupValues[2]
            kotlinx.coroutines.runBlocking { sendMailRequest?.send(recipient, message) }
        },

        // send item(s) to recipient [|| message]
        Regex("^send\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliSend(m.groupValues[1], isMeat = false, rt)
        },

        // csend meat to recipient [|| message]
        Regex("^csend\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliSend(m.groupValues[1], isMeat = true, rt)
        },

        // gift item(s) to recipient [|| message] — town_sendgift.php
        Regex("^gift\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliGift(m.groupValues[1], rt)
        },

        // note — print user note; note text — save user note
        Regex("^note$", RegexOption.IGNORE_CASE) to { _, rt ->
            rt.print(preferences?.getString(Preferences.USER_NOTE, "") ?: "")
        },
        Regex("^note\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, _ ->
            preferences?.setString(Preferences.USER_NOTE, m.groupValues[1])
        },

        // absorb [item] — Gelatinous Noob absorb (no args: refresh count)
        Regex("^absorb(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            val params = m.groupValues[1].trim()
            kotlinx.coroutines.runBlocking {
                if (params.isEmpty()) {
                    val request = absorbRequest
                    if (request != null) {
                        request.refreshAbsorbs()
                            .onSuccess { rt.print(it.toString()) }
                            .onFailure { rt.print(it.message ?: "absorb failed") }
                    } else {
                        rt.print((character?.state?.value?.absorbs ?: 0).toString())
                    }
                } else {
                    absorbRequest?.absorb(params)
                        ?.onFailure { rt.print(it.message ?: "absorb failed") }
                }
            }
        },

        // version / cli — print mobile revision string
        Regex("^(?:version|cli)$", RegexOption.IGNORE_CASE) to { _, rt ->
            runVersionCli(rt)
        },
        Regex("^greyyou(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runGreyYouCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        // Phases 1023–1032 Familiar / path CLI Track B
        Regex("^absorptions(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runAbsorptionsCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^gooskills(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runGooSkillsCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^bugbears$", RegexOption.IGNORE_CASE) to { _, rt ->
            runBugbearsCli(rt)
        },
        Regex("^chibi(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runChibiCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^panda(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runPandaCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^devilcandyegg(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runDevilCandyEggCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^train(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runTrainFamiliarCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },

        // charpane — visit character pane
        Regex("^charpane$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("charpane.php")
        },

        // run / call / exec / execute / load / start / validate / verify / profile — saved ASH
        Regex("^(?:run(?:script)?|call|execute|exec|load|start|validate|verify|profile)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val verb = m.value.substringBefore(' ').trim()
            when {
                verb.equals("validate", ignoreCase = true) ||
                    verb.equals("verify", ignoreCase = true) ||
                    verb.equals("profile", ignoreCase = true) ->
                    cliValidateOrProfileScript(verb, m.groupValues[1].trim(), rt)
                else -> runCallScriptCli(m.groupValues[1].trim(), rt)
            }
        },

        Regex("^maximizer$", RegexOption.IGNORE_CASE) to { _, rt ->
            rt.print("Usage: maximize <goal>  (e.g. maximize mysticality)")
        },
        Regex("^maximize(?:\\s+(.+))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            val goal = m.groupValues.getOrNull(1)?.trim().orEmpty().ifBlank { "all" }
            val mgr = maximizerManager ?: return@to
            val result = kotlinx.coroutines.runBlocking { mgr.maximize(goal) }
            rt.print(if (result.success) "Maximized for $goal" else "No improvement for $goal")
        },

        Regex("^(?:speculate|whatif)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val goal = m.groupValues[1].trim()
            val mgr = maximizerManager ?: run {
                rt.print("Maximizer unavailable")
                return@to
            }
            val lines = kotlinx.coroutines.runBlocking { mgr.speculate(goal) }
            lines.forEach { rt.print(it) }
        },

        Regex("^umbrella(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            dispatchModeableCli(
                Modeable.UMBRELLA,
                ModeableRequest.normalizeUmbrellaParameter(m.groupValues.getOrNull(1).orEmpty()),
                rt,
            )
        },
        Regex("^kgb(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliKgb(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^parka(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            val mode = ModeableRequest.normalizeParkaParameter(m.groupValues.getOrNull(1).orEmpty())
            dispatchModeableCli(Modeable.PARKA, mode, rt)
        },
        Regex("^backupcamera(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            dispatchModeableCli(Modeable.BACKUPCAMERA, m.groupValues.getOrNull(1)?.trim().orEmpty(), rt)
        },
        Regex("^edpiece(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            dispatchModeableCli(Modeable.EDPIECE, m.groupValues.getOrNull(1)?.trim().orEmpty(), rt)
        },
        Regex("^retrocape(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            dispatchModeableCli(Modeable.RETROCAPE, m.groupValues.getOrNull(1)?.trim().orEmpty(), rt)
        },
        Regex("^snowsuit(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            dispatchModeableCli(Modeable.SNOWSUIT, m.groupValues.getOrNull(1)?.trim().orEmpty(), rt)
        },
        Regex("^(?:ledcandle|jillcandle)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            dispatchModeableCli(
                Modeable.LED_CANDLE,
                ModeableRequest.normalizeLedCandleParameter(m.groupValues.getOrNull(1).orEmpty()),
                rt,
            )
        },

        Regex("^guzzlr\\s+abandon$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliGuzzlrAbandon(rt)
        },
        Regex("^guzzlr\\s+accept\\s+(bronze|gold|platinum)$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliGuzzlrAccept(m.groupValues[1].lowercase(), rt)
        },

        Regex("^maze\\s+(traps|gopher|duck|chihuahua|kiwi|nugglets)$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliMaze(m.groupValues[1], rt)
        },

        Regex("^door$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliDoor(rt)
        },

        // autoscript on/off — persist preference stub
        Regex("^autoscript\\s+(on|off)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val on = m.groupValues[1].equals("on", ignoreCase = true)
            preferences?.setBoolean(Preferences.AUTO_SCRIPTING, on)
            rt.print(if (on) "autoscript enabled" else "autoscript disabled")
        },

        // sync — alias for full refresh (character + quest log)
        Regex("^sync$", RegexOption.IGNORE_CASE) to { _, rt ->
            dispatchCli("refresh", rt)
        },

        // recover / restore / check hp|mp|both — desktop RecoverCommand
        Regex("^(?:recover|restore|check)\\s+(hp|health|mp|mana|both)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runRecoverCli(m.groupValues[1], rt)
        },
        // recover / restore / check — force recovery loop once (`rest` is campground)
        Regex("^(?:recover|restore|check)$", RegexOption.IGNORE_CASE) to { _, _ ->
            val rm = recoveryManager ?: return@to
            val char = character ?: return@to
            kotlinx.coroutines.runBlocking {
                rm.recoverIfNeeded(
                    charState  = char.state.value,
                    invState   = inventoryManager?.state?.value ?: InventoryState(),
                    skillState = skillManager?.state?.value ?: SkillState(),
                    force      = true,
                )
                characterRequest?.fetchCharacterState()?.onSuccess { char.updateFromApiResponse(it) }
            }
        },

        // takeshop N item
        Regex("^takeshop\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { manageStoreRequest?.removeItem(itemId, qty) }
        },

        // empty closet
        Regex("^empty\\s+closet$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking {
                refreshClosetCacheAfter(closetRequest?.emptyCloset())
            }
        },

        // overdrink [N] item — qty optional; same drink path as drinksilent on mobile
        Regex("^overdrink(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliDrink(m.groupValues.getOrNull(1)?.trim().orEmpty(), rt::print)
        },

        // echo / print — output text to CLI stream (`timestamp` → KoL calendar day)
        Regex("^(?:echo|print)\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runEchoCli(m.groupValues[1], rt)
        },
        Regex("^(?:colorecho|cecho)\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runColorEchoCli(m.groupValues[1], rt)
        },
        Regex("^text\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runVisitUrlCli(m.groupValues[1].trim(), printHtml = true, rt)
        },
        Regex("^mpitems$", RegexOption.IGNORE_CASE) to { _, rt ->
            runMpItemsCli(rt)
        },
        Regex("^restores(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runRestoresCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^rest(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runRestCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^events(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runEventsCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^prefref(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runPrefRefCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^(?:help|which)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runHelpCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^modref(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runModRefCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^condref$", RegexOption.IGNORE_CASE) to { _, rt ->
            runCondRefCli(rt)
        },
        Regex("^reminisce(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runReminisceCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^poolskill$", RegexOption.IGNORE_CASE) to { _, rt ->
            runPoolSkillCli(rt)
        },
        Regex("^insults$", RegexOption.IGNORE_CASE) to { _, rt ->
            runInsultsCli(rt)
        },

        // status — desktop ShowDataCommand multi-line dump
        Regex("^status$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliStatus(rt)
        },

        // modifiers [filter] — desktop ShowDataCommand combat-stat dump
        Regex("^modifiers(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliModifiers(m.groupValues.getOrNull(1).orEmpty(), rt)
        },

        // Phases 1043–1052 CLI Track D — session / store / script aliases
        Regex("^(?:undercut)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliUndercut(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^reprice(?:\\s+min)?$", RegexOption.IGNORE_CASE) to { m, rt ->
            val params = if (m.value.contains("min", ignoreCase = true)) "min" else ""
            cliUndercut(params, rt)
        },
        Regex("^(?:timein|relog|relogin)$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliTimein(rt)
        },
        Regex("^encounters$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliEncounters(rt)
        },
        Regex("^session$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliSession(rt)
        },
        Regex("^summary$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliSummary(rt)
        },
        Regex("^modifies(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliModifies(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^location(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliLocation(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^cache(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliCache(m.groupValues.getOrNull(1).orEmpty(), rt)
        },

        // moon / moons — desktop ShowDataCommand holiday/moon dump
        Regex("^moons?$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliMoon(rt)
        },
        Regex("^accordions$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliAccordions(rt)
        },
        Regex("^actionbar(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliActionBar(m.groupValues.getOrNull(1).orEmpty(), rt::print)
        },

        // logecho / logprint — session log only (desktop LogEchoCommand)
        Regex("^log(?:echo|print)\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, _ ->
            runLogEchoCli(m.groupValues[1])
        },
        // fecho / fprint — print + session log (desktop FullEchoCommand)
        Regex("^(?:fecho|fprint)\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runFullEchoCli(m.groupValues[1], rt)
        },

        // log snapshot | log a, b, c — desktop PlayerSnapshotCommand
        Regex("^log\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliLogSnapshot(m.groupValues[1], rt)
        },

        // abort [message] — stop scripts/automation (desktop AbortCommand)
        Regex("^abort(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runAbortCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        // repeat [N] — replay previous CLI line (desktop RepeatLineCommand)
        Regex("^repeat(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runRepeatCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        // stop / pause — cancel running adventure loop and maximizer search
        Regex("^(?:stop|pause)$", RegexOption.IGNORE_CASE) to { _, _ ->
            net.sourceforge.kolmafia.maximizer.MaximizerContinuation.abort()
            adventureManager?.stop()
        },

        Regex("^is_adventuring$", RegexOption.IGNORE_CASE) to { _, rt ->
            val running = adventureManager?.isRunning?.value == true
            rt.print(if (running) "true" else "false")
        },
        Regex("^has_queued_commands$", RegexOption.IGNORE_CASE) to { _, rt ->
            rt.print("false")
        },

        Regex("^partyfair$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("place.php?whichplace=partyfair", applyQuestHooks = true)
        },

        Regex("^war$", RegexOption.IGNORE_CASE) to { _, rt ->
            val progress = preferences?.getString("warProgress", "unstarted") ?: "unstarted"
            rt.print(progress)
        },

        Regex("^telescope(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliTelescope(m.groupValues.getOrNull(1)?.trim().orEmpty()) { message -> rt.print(message) }
        },

        // main / council / campground / homepage — visit common KoL pages
        Regex("^main$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("main.php")
        },
        Regex("^homepage$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("main.php")
        },
        Regex("^council$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("council.php", applyQuestHooks = true)
        },
        Regex("^campground$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("campground.php")
        },
        Regex("^camp$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("campground.php")
        },

        // breakfast — full daily sequence, or skills/books-only subcommands
        Regex("^breakfast(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            val mgr = breakfastManager ?: return@to
            val char = character ?: return@to
            val inv = inventoryManager ?: return@to
            val sub = m.groupValues.getOrNull(1)?.trim().orEmpty().lowercase()
            kotlinx.coroutines.runBlocking {
                when {
                    sub.isEmpty() -> mgr.runBreakfast(char.state.value, inv.state.value)
                    sub == "skills" -> mgr.castSkills(char.state.value)
                    sub == "books" -> mgr.castBookSkills(char.state.value)
                    else -> rt.print("Usage: breakfast [skills|books]")
                }
            }
        },

        // wiki / javadoc / lookup — print Kol Wiki URL (headless has no browser)
        Regex("^lookup(?:\\s+(effect|familiar|item|skill|outfit|monster|location))?\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            rt.print(wikiUrlFor(m.groupValues[2].trim()))
        },
        Regex("^wiki\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            rt.print(wikiUrlFor(m.groupValues[1].trim()))
        },
        Regex("^ashwiki(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runAshWikiCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^safe(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runSafeCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^monsters(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runMonstersCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^locations$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliLocations(rt)
        },
        Regex("^javadoc\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            rt.print(wikiUrlFor(m.groupValues[1].trim()))
        },

        // turns / turnsleft — print adventures remaining
        Regex("^(?:turns|turnsleft)$", RegexOption.IGNORE_CASE) to { _, rt ->
            rt.print((character?.state?.value?.adventuresLeft ?: 0).toString())
        },

        // relay on/off/status — headless stub; scripts check pref only
        Regex("^relay(?:\\s+(on|off|open|close|status))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            when (m.groupValues.getOrNull(1)?.lowercase()) {
                "on", "open" -> preferences?.setBoolean("relayActive", true)
                "off", "close" -> preferences?.setBoolean("relayActive", false)
                "status" -> {
                    val active = preferences?.getBoolean("relayActive", false) == true
                    rt.print(if (active) "Relay is on." else "Relay is off.")
                }
                else -> rt.print("Relay is not available in KoLmafia Mobile.")
            }
        },

        // hermit N item — trade with the hermit (qty form first so first-match wins)
        Regex("^hermit\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val qty = m.groupValues[1].toIntOrNull() ?: return@to
            runHermitTradeCli(m.groupValues[2], qty, rt)
        },
        Regex("^hermit$", RegexOption.IGNORE_CASE) to { _, rt ->
            runHermitStatusCli(rt)
        },
        Regex("^hermit\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runHermitTradeCli(m.groupValues[1], 1, rt)
        },

        // config get/set — aliases for get/set prefs
        Regex("^config\\s+get\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val value = preferences?.getString(m.groupValues[1].trim(), "") ?: ""
            rt.print(value)
        },
        Regex("^config\\s+set\\s+(\\S+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            preferences?.setString(m.groupValues[1].trim(), m.groupValues[2])
        },

        // put_closet N item — alias for closet put
        Regex("^put_closet\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking {
                refreshClosetCacheAfter(closetRequest?.putIn(itemId, qty))
            }
        },

        // take_closet N item — alias for closet take
        Regex("^take_closet\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking {
                refreshClosetCacheAfter(closetRequest?.takeOut(itemId, qty))
            }
        },

        // take_storage N item — alias for storage take
        Regex("^take_storage\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking {
                refreshStorageCacheAfter(storageRequest?.withdraw(itemId, qty))
            }
        },

        // pull / hagnk all — empty storage (desktop StorageCommand)
        Regex("^(?:pull|hagnk)\\s+all$", RegexOption.IGNORE_CASE) to { _, rt ->
            runPullAllCli(rt)
        },
        // pull / hagnk outfit <name>
        Regex("^(?:pull|hagnk)\\s+outfit\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runPullOutfitCli(m.groupValues[1], rt)
        },
        // pull / hagnk qty-optional comma item lists
        Regex("^(?:pull|hagnk)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runPullCli(m.groupValues[1].trim(), rt)
        },

        // budget [N] — show/set Hagnk's pulls budgeted for automatic use
        Regex("^budget(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runBudgetCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },

        // searchmall [item] [with limit N] — live mall price/qty rows
        Regex("^searchmall(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runSearchMallCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },

        Regex("^grandpa(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runGrandpaCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },

        Regex("^donate(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runDonateCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },

        Regex("^raffle(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runRaffleCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },

        // reprice N item[@limit] — mall store reprice
        Regex("^reprice\\s+(\\d+)\\s+(.+?)(?:@(\\d+))?$", RegexOption.IGNORE_CASE) to { m, _ ->
            val price = m.groupValues[1].toIntOrNull() ?: return@to
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            val limit = m.groupValues[3].toIntOrNull() ?: 0
            kotlinx.coroutines.runBlocking { manageStoreRequest?.repriceItem(itemId, price, limit) }
        },

        // checkpoint / checkpoint clear — save or clear outfit checkpoint
        Regex("^checkpoint(?:\\s+clear)?$", RegexOption.IGNORE_CASE) to { m, _ ->
            if (m.value.endsWith("clear", ignoreCase = true)) {
                OutfitCheckpoint.clearSaved()
                return@to
            }
            val char = character ?: return@to
            val equip = equipmentRequest ?: return@to
            val db = gameDatabase ?: return@to
            kotlinx.coroutines.runBlocking {
                OutfitCheckpoint.snapshot(char, equip, db)
            }
        },

        // uneffect name / uneffect all
        Regex("^uneffect\\s+all$", RegexOption.IGNORE_CASE) to { _, _ ->
            uneffectAll()
        },
        Regex("^uneffect\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            uneffectParameter(m.groupValues[1].trim())
        },

        // shrug / remedy — aliases for uneffect
        Regex("^(?:shrug|remedy)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            uneffectParameter(m.groupValues[1].trim())
        },

        // dump / dump off — compact state summary
        Regex("^dump(?:\\s+off)?$", RegexOption.IGNORE_CASE) to { _, rt ->
            rt.print(buildDumpSummary())
        },

        // batch open / batch close — pref counter only
        Regex("^batch\\s+open$", RegexOption.IGNORE_CASE) to { _, _ ->
            val prefs = preferences ?: return@to
            prefs.setInt("batching", (prefs.getInt("batching", 0) + 1).coerceAtLeast(1))
        },
        Regex("^batch\\s+close$", RegexOption.IGNORE_CASE) to { _, _ ->
            val prefs = preferences ?: return@to
            val current = prefs.getInt("batching", 0)
            if (current > 0) prefs.setInt("batching", current - 1)
        },

        // reagent N item
        Regex("^reagent\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { useItemRequest?.use(itemId, qty) }
        },

        // goal factoid text — stop when response contains text
        Regex("^goal\\s+factoid\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            goalManager?.setFactoidGoal(m.groupValues[1].trim())
        },

        // goal autostop text — desktop alias for factoid goal
        Regex("^goal\\s+autostop\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            goalManager?.setFactoidGoal(m.groupValues[1].trim())
        },

        // putshop price[@limit] N item — list item in mall store
        Regex("^putshop\\s+(\\d+)(?:@(\\d+))?\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val price = m.groupValues[1].toIntOrNull() ?: return@to
            val limit = m.groupValues[2].toIntOrNull() ?: 0
            val qty = m.groupValues[3].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[4].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking {
                manageStoreRequest?.addItem(itemId, price, limit, qty)
            }
        },

        // wear / wield / equip list — desktop EquipCommand ShowData dump
        Regex("^(?:equip|wear|wield)$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliEquipment("", rt)
        },
        Regex("^(?:equip|wear|wield)\\s+list(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliEquipment(m.groupValues.getOrNull(1).orEmpty(), rt)
        },

        // wear / wield — aliases for equip
        Regex("^(?:wear|wield)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliEquip(m.groupValues[1].trim(), rt)
        },
        Regex("^(?:second|hold|dualwield)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliEquip("offhand ${m.groupValues[1].trim()}", rt)
        },

        // "equip [<slot>] <item-name>" — equip item, optionally into a named slot.
        Regex("^equip\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliEquip(m.groupValues[1].trim(), rt)
        },

        // unequip / remove — bare, all, slot, or item-name substring
        Regex("^(?:unequip|remove)$", RegexOption.IGNORE_CASE) to { _, rt ->
            runUnequipCli("", rt)
        },
        Regex("^(?:unequip|remove)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runUnequipCli(m.groupValues[1].trim(), rt)
        },

        // pulverize|smash [N] item[, item]... — smash equipment into smithing materials
        Regex("^(?:pulverize|smash)\\s+(?:(\\d+)\\s+)?(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val explicitQty = m.groupValues[1].toIntOrNull()
            val itemNames = m.groupValues[2].split(',').map { it.trim() }.filter { it.isNotBlank() }
            kotlinx.coroutines.runBlocking { runPulverizeCli(itemNames, explicitQty) }
        },

        // zap item[, item]... — transform items with wand
        Regex("^zap\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val itemNames = m.groupValues[1].split(',').map { it.trim() }.filter { it.isNotBlank() }
            kotlinx.coroutines.runBlocking { runZapCli(itemNames) }
        },

        Regex("^(?:fold|squeeze)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            kotlinx.coroutines.runBlocking { runFoldCli(m.groupValues[1].trim(), rt) }
        },
        Regex("^(?:waitq)\\s*(.*)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runWaitCli(m.groupValues[1], quiet = true, rt)
        },
        Regex("^wait\\s*(.*)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runWaitCli(m.groupValues[1], quiet = false, rt)
        },
        Regex("^banishes$", RegexOption.IGNORE_CASE) to { _, rt ->
            runBanishesCli(rt)
        },
        Regex("^(recipe|ingredients)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runRecipeCli(m.groupValues[1], m.groupValues[2], rt)
        },
        Regex("^(olfact|olfaction|putty)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runOlfactCli(m.groupValues[1], m.groupValues[2], rt)
        },
        Regex("^holiday(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runHolidayCli(m.groupValues[1], rt)
        },
        Regex("^garden(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            kotlinx.coroutines.runBlocking { runGardenCli(m.groupValues[1], rt) }
        },
        Regex("^ashq(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runAshCli(m.groupValues.getOrNull(1).orEmpty(), quiet = true, rt)
        },
        Regex("^ashref(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runAshRefCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^ash(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runAshCli(m.groupValues.getOrNull(1).orEmpty(), quiet = false, rt)
        },
        Regex("^(?:aa|autoattack)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runAutoAttackCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^bounty(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runBountyCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^saber(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            kotlinx.coroutines.runBlocking { runSaberCli(m.groupValues.getOrNull(1).orEmpty(), rt) }
        },
        Regex("^snapper(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            kotlinx.coroutines.runBlocking { runSnapperCli(m.groupValues.getOrNull(1).orEmpty(), rt) }
        },
        Regex("^(?:eudora|correspondent)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runEudoraCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^mayominder(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            kotlinx.coroutines.runBlocking { runMayoMinderCli(m.groupValues.getOrNull(1).orEmpty(), rt) }
        },
        Regex("^(?:bang|!)$", RegexOption.IGNORE_CASE) to { _, rt ->
            runBangPotionsCli(vials = false, rt)
        },
        Regex("^vials$", RegexOption.IGNORE_CASE) to { _, rt ->
            runBangPotionsCli(vials = true, rt)
        },
        Regex("^up(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runUpCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^spoon(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            kotlinx.coroutines.runBlocking { runSpoonCli(m.groupValues.getOrNull(1).orEmpty(), rt) }
        },
        Regex("^dusty$", RegexOption.IGNORE_CASE) to { _, rt ->
            runDustyCli(rt)
        },
        Regex("^chips(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runChipsCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^(?:sofa|sleep)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runSofaCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^crimbotree(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runCrimboTreeCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^burn(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runBurnCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^(?:kitchen|hellkitchen|hellskitchen)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runNamedCafeCli(LongTailCli.NamedCafe.KITCHEN, m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^restaurant(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runNamedCafeCli(LongTailCli.NamedCafe.RESTAURANT, m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^(?:brewery|microbrewery)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runNamedCafeCli(LongTailCli.NamedCafe.BREWERY, m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^mallsell(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runMallSellCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^shop(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runShopCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^stickers(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runStickersCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^folders(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runFoldersCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^ocean(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runOceanCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^cardsleeve(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runCardsleeveCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },
        Regex("^bootskin(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runBootSubSlotCli(
                EquipmentSlot.BOOTSKIN,
                m.groupValues.getOrNull(1).orEmpty(),
                rt,
            )
        },
        Regex("^bootspur(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runBootSubSlotCli(
                EquipmentSlot.BOOTSPUR,
                m.groupValues.getOrNull(1).orEmpty(),
                rt,
            )
        },
        Regex("^(?:condition|objective|conditions|objectives)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runConditionCli(m.groupValues.getOrNull(1).orEmpty(), rt)
        },

        // cleanup / junk — untinker, use boxes, pulverize, autosell junk list
        Regex("^cleanup(?:\\s+junk)?$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking { runCleanupJunkCli() }
        },
        Regex("^junk$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking { runCleanupJunkCli() }
        },

        // untinker — complete screwdriver quest (no args)
        Regex("^untinker$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking { runUntinkerQuestCli() }
        },

        // untinker item[, item]... — break apart meat-paste items at the Untinker
        Regex("^untinker\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val itemNames = m.groupValues[1].split(',').map { it.trim() }.filter { it.isNotBlank() }
            kotlinx.coroutines.runBlocking { runUntinkerCli(itemNames) }
        },

        // automall — mall all profitable non-memento inventory items
        Regex("^automall$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking { runAutoMallCli() }
        },

        // quark [item[, item]...] — paste unstable quark with best junk-list item
        Regex("^quark(?:\\s+(.+))?$", RegexOption.IGNORE_CASE) to { m, _ ->
            val params = m.groupValues[1].trim()
            val itemNames = if (params.isEmpty()) {
                emptyList()
            } else {
                params.split(',').map { it.trim() }.filter { it.isNotBlank() }
            }
            kotlinx.coroutines.runBlocking { runQuarkCli(itemNames) }
        },

        // "sell|autosell [qty] item [, ...]" — autosell from inventory
        Regex("^(?:sell|autosell)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, _ ->
            runAutosellCli(m.groupValues.getOrNull(1).orEmpty())
        },

        // "outfit save <name>" — save current equipment as custom outfit
        Regex("^outfit\\s+save\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val name = m.groupValues[1].trim()
            kotlinx.coroutines.runBlocking { outfitManager?.saveOutfit(name) }
        },

        // "outfit list [filter]" — print outfit names matching optional filter
        Regex("^outfit\\s+list(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliOutfits(m.groupValues.getOrNull(1).orEmpty(), rt)
        },

        // "outfit" — desktop ShowDataCommand lists owned outfits without wearing
        Regex("^outfit$", RegexOption.IGNORE_CASE) to { _, rt ->
            cliOutfits("", rt)
        },

        // "outfit checkpoint" — restore equipment from last checkpoint
        Regex("^outfit\\s+checkpoint$", RegexOption.IGNORE_CASE) to { _, _ ->
            val char = character ?: return@to
            val equip = equipmentRequest ?: return@to
            val db = gameDatabase ?: return@to
            kotlinx.coroutines.runBlocking {
                OutfitCheckpoint.restoreSaved(equip, db)
            }
        },

        // "outfit <name>" — wear named outfit (runs embedded c=/e=/f= actions after wear)
        Regex("^outfit\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val name = m.groupValues[1].trim()
            if (name.equals("list", ignoreCase = true) ||
                name.equals("checkpoint", ignoreCase = true) ||
                name.startsWith("save ", ignoreCase = true)
            ) return@to
            kotlinx.coroutines.runBlocking {
                outfitManager?.wearOutfit(name) { cmd -> dispatchCli(cmd, rt) }
            }
        },

        // "buy using storage N ¶itemId[@limit]" — HC/Ronin mall buy tracked via storage
        Regex("^buy\\s+using\\s+storage\\s+(\\d+)\\s+[\\u00B6¶](\\d+)(?:@(\\d+))?$", RegexOption.IGNORE_CASE) to { m, _ ->
            val count = m.groupValues[1].toIntOrNull() ?: return@to
            val itemId = m.groupValues[2].toIntOrNull() ?: return@to
            val limit = m.groupValues[3].toIntOrNull() ?: Int.MAX_VALUE
            val mall = mallManager ?: return@to
            val char = character ?: return@to
            val equip = equipmentRequest ?: return@to
            val db = gameDatabase ?: return@to
            val cs = char.state.value
            if (!cs.isHardcore && !cs.isInRonin) return@to
            kotlinx.coroutines.runBlocking {
                val initial = storageRequest?.fetchContents()?.get(itemId) ?: 0
                val checkpoint = OutfitCheckpoint.snapshot(char, equip, db)
                checkpoint.use { mall.buy(itemId, count, limit) }
                storageRequest?.fetchContents()?.get(itemId) ?: initial
            }
        },

        // "coinmaster buy N <nick> <item>" — quantity before nickname (legacy)
        Regex("^coinmaster\\s+buy\\s+(\\d+)\\s+(\\S+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: return@to
            val nickname = m.groupValues[2].trim()
            val itemName = m.groupValues[3].trim()
            val master = coinmasterManager?.resolveMaster(nickname) ?: return@to
            val itemId = resolveMallBuyItemId(itemName) ?: return@to
            kotlinx.coroutines.runBlocking { coinmasterManager?.buy(master, itemId, qty) }
        },

        // "coinmaster buy|sell <nick> [qty] item [, …]" — qty-optional comma lists
        Regex("^coinmaster\\s+(buy|sell)\\s+(\\S+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val isBuy = m.groupValues[1].equals("buy", ignoreCase = true)
            runCoinmasterTradeCli(isBuy, m.groupValues[2].trim(), m.groupValues[3].trim())
        },

        // create / make / bake / mix / smith / tinker / ply — bare list or qty-optional items
        Regex("^(?:create|make|bake|mix|smith|tinker|ply)$", RegexOption.IGNORE_CASE) to { _, rt ->
            runCreateCli("", rt)
        },
        Regex("^(?:create|make|bake|mix|smith|tinker|ply)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            runCreateCli(m.groupValues[1].trim(), rt)
        },

        // logout / exit / quit — clear session state (login/timein/relog deferred)
        Regex("^(?:logout|exit|quit|timeout)$", RegexOption.IGNORE_CASE) to { _, _ ->
            sessionManager?.logout()
        },

        // "buy|mallbuy [from mall] [qty] item [@limit] [, ...]" — NPC or mall purchase
        Regex("^(buy|mallbuy)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            runBuyCli(m.groupValues[1], m.groupValues.getOrNull(2).orEmpty(), rt)
        },
    )

    internal fun resolveLocation(name: String): AdventureLocation? {
        AdventureDatabase.getByName(name)?.let { return it.toLocation() }
        LocationDatabase.ALL_LOCATIONS.find { it.name.equals(name, ignoreCase = true) }?.let {
            return AdventureLocation(it.snarfblat, it.name, it.zone)
        }
        LocationDatabase.findBySnarfblat(name)?.let {
            return AdventureLocation(it.snarfblat, it.name, it.zone)
        }
        AdventureDatabase.getBySnarfblat(name)?.let { return it.toLocation() }
        if (name.isNotEmpty() && name.all { it.isDigit() }) {
            return AdventureLocation(name, name, "")
        }
        return null
    }

    internal fun wikiUrlFor(name: String): String {
        val slug = name.trim().replace(' ', '_')
        return "https://wiki.a.kolmafia.us/wiki/$slug"
    }

    internal fun applyItemUseResponse(itemId: Int, html: String) {
        when (itemId) {
            AirportSync.SPRING_BEACH_TICKET ->
                preferences?.let { AirportSync.syncFromSpringBeachTicketUse(html, it) }
            in FamiliarSoupSync.protogeneticSoupIds ->
                FamiliarSoupSync.applyProtogeneticSoupUse(
                    itemId = itemId,
                    html = html,
                    familiarId = character?.state?.value?.familiarId ?: 0,
                    familiarManager = familiarManager,
                )
        }
    }

    internal fun processVisitResponseHooks(html: String, url: String? = null) {
        val normalizedUrl = url.orEmpty()
            .removePrefix(KOL_BASE_URL)
            .removePrefix("/")
        val choiceId = WHICH_CHOICE_URL_PATTERN.find(normalizedUrl)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        processVisitResponseHooksForPath(normalizedUrl, html, choiceId)

        net.sourceforge.kolmafia.request.MonsterManuelRequest.parseResponse(url, html)
        if (url?.contains("town_right.php", ignoreCase = true) == true) {
            GourdRequest.parseResponse(url, html, preferences, inventoryManager)
            preferences?.let { VoteMonsterManager.applyFromVisit(url, html, it) }
        }
        if (url?.contains("whichchoice=", ignoreCase = true) == true) {
            Regex("""whichchoice=(\d+)""", RegexOption.IGNORE_CASE).find(url)
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?.takeIf { it in ChibiBuddyManager.CHOICE_IDS }
                ?.let { choice ->
                    val decision = Regex("""(?:option|decision)=(\d+)""", RegexOption.IGNORE_CASE)
                        .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                    if (decision == 0) {
                        ChibiBuddyManager.visit(choice, html, preferences, character, inventoryManager)
                    } else {
                        ChibiBuddyManager.postChoice(
                            choice,
                            decision,
                            html,
                            preferences,
                            inventoryManager,
                            character,
                        )
                    }
                }
        }
        if (url != null) {
            RequestLogger.registerRequest(url, sessionLogger, preferences)
            NemesisRequest.parseResponse(
                url = url,
                html = html,
                preferences = preferences,
                inventory = inventoryManager,
                questDatabase = questDatabase,
            )
            DvorakManager.parseResponse(url, html)
            when {
                url.contains("messages.php", ignoreCase = true) ||
                    url.contains("mail.php", ignoreCase = true) -> {
                    val mailbox = Regex("""(?:box|mailbox)=([^&]+)""", RegexOption.IGNORE_CASE)
                        .find(url)?.groupValues?.get(1) ?: "Inbox"
                    MailManager.parseMailbox(mailbox, html)
                }
                url.contains("account_contactlist.php", ignoreCase = true) ->
                    ContactManager.updateFromHtml(html)
            }
            TavernRequest.parseResponse(
                url = url,
                html = html,
                preferences = preferences,
                questDatabase = questDatabase,
                ascensionNumber = character?.state?.value?.ascensionNumber ?: 0,
                consumeItem = { itemId, quantity ->
                    inventoryManager?.consumeItemLocally(itemId, quantity)
                },
                spendMeat = { amount ->
                    ResultProcessor.processMeat(-amount, character)
                },
            )
            SpadingManager.processPlace(url, html, preferences, sessionLogger)
            if (url.contains("whichplace=nstower", ignoreCase = true) &&
                SorceressLairSync.action(url) == "ns_10_sorcfight"
            ) {
                SorceressLairSync.enterSorceressFight(effectManager)
            }
        }
        EventHistory.checkForNewEvents(html)
        val questHubRouted = url?.let { QuestManager.handles(it) } == true
        if (questHubRouted) {
            QuestManager.handleQuestChange(
                url = url!!,
                html = html,
                ctx = QuestManager.QuestChangeContext(
                    preferences = preferences,
                    questDatabase = questDatabase,
                    characterState = character?.state?.value,
                    inventoryManager = inventoryManager,
                    gameDatabase = gameDatabase,
                    sessionLogger = sessionLogger,
                    clearEquipment = { slot -> character?.updateEquipment(slot, "") },
                    adventureTurns = { name -> adventureSpentTracker?.getTurns(name) ?: 0 },
                    parseQuestLogPage = { page, body ->
                        questLogRequest?.parsePage(
                            body,
                            page,
                            character?.state?.value?.ascensionNumber ?: 0,
                        )
                    },
                ),
            )
        }
        if (url?.contains("leaflet.php", ignoreCase = true) == true) {
            LeafletManager.parseLocation(html)
        }
        if (url?.contains("whichchoice=${WumpusManager.CHOICE_ID}", ignoreCase = true) == true) {
            val decision = Regex("""(?:option|decision)=(\d+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            WumpusManager.applyChoice(decision, html)
        }
        if (url != null && (
                url.contains("whichchoice=${RabbitHoleManager.TEA_PARTY_CHOICE}", true) ||
                    url.contains("whichchoice=${RabbitHoleManager.RABBIT_HOLE_CHOICE}", true) ||
                    url.contains("whichchoice=${RabbitHoleManager.CHESS_CHOICE}", true)
                )
        ) {
            val choiceId = Regex("""whichchoice=(\d+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val decision = Regex("""(?:option|decision)=(\d+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            RabbitHoleManager.applyChoice(
                choiceId, decision, url, html, preferences,
            ) { id, qty -> inventoryManager?.consumeItemLocally(id, qty) }
        }
        if (url?.contains("charsheet.php", ignoreCase = true) == true) {
            GreyYouManager.parseAbsorptions(
                html,
                character?.state?.value?.ascensionPath == AscensionPath.GREY_YOU,
            ) { sessionLogger?.appendRawLine(it) }
        }
        if (url?.contains("afterlife.php", ignoreCase = true) == true) {
            AfterLifeRequest.registerRequest(url, sessionLogger)
            AfterLifeRequest.parseResponse(url, html, preferences, sessionLogger)
            if (url.contains("confirmascend=1")) {
                AfterLifeRequest.handleAscensionConfirm(url, character, preferences, banishManager)
            }
        }
        if (url?.contains("ascend.php", ignoreCase = true) == true &&
            url.contains("confirm=1", ignoreCase = true)
        ) {
            ValhallaManager.onAscension(character, preferences, banishManager)
        }
        if (url?.startsWith("spaaace.php") == true) {
            SpaaaceRequest.registerRequest(url, sessionLogger)
            SpaaaceRequest.parseResponse(url, html, questDatabase)
            if (url.contains("action=playporko", ignoreCase = true)) {
                SpaaaceRequest.visitPorkoChoice(html, preferences) { itemId, delta ->
                    if (delta < 0) inventoryManager?.consumeItemLocally(itemId, -delta)
                }
            }
            if (url.contains("whichchoice=", ignoreCase = true) &&
                html.contains("peg style", ignoreCase = true)
            ) {
                SpaaaceRequest.visitGeneratorChoice(html, preferences)
            }
        }
        if (url?.contains("main.php", ignoreCase = true) == true &&
            url.contains("comb=1", ignoreCase = true)
        ) {
            BeachCombChoiceSync.apply(
                BeachCombChoiceSync.CHOICE_ID,
                0,
                html,
                preferences,
                url,
            )
        }
        if (url?.contains("familiar.php", ignoreCase = true) == true &&
            url.contains("ajax=1", ignoreCase = true) != true
        ) {
            FamiliarSoupSync.apply(html, familiarManager)
        } else if (FamiliarSoupSync.containsSoupComment(html)) {
            FamiliarSoupSync.apply(html, familiarManager)
        }
        if (url?.contains("famteam.php", ignoreCase = true) == true) {
            FamTeamSync.registerRequest(url, sessionLogger)
            PokefamBoostSync.syncFromFeed(url, html, preferences, inventoryManager)
            character?.let { char ->
                if (char.state.value.inPokefam) {
                    FamTeamSync.apply(char, html, familiarManager, preferences, sessionLogger)
                }
            }
        }
        if (url?.contains("qterrarium.php", ignoreCase = true) == true) {
            val char = character
            if (char != null) {
                QuantumTerrariumRequest.parseVisit(
                    url = url,
                    html = html,
                    character = char,
                    preferences = preferences,
                    sessionLogger = sessionLogger,
                )
            }
        }
        if (url?.contains("wildfire", ignoreCase = true) == true ||
            html.contains("wildfire_captain", ignoreCase = true)
        ) {
            wildfireCampManager?.parseCaptain(html)
        }
        if (url != null && url.contains("whichchoice=${CargoPocketSync.CARGO_CULT_CHOICE}")) {
            if (url.contains("pocket=") &&
                (html.contains("You're fighting") || html.contains("fight.php"))
            ) {
                cargoPocketSync?.registerPocketFight(url)
            }
            cargoPocketSync?.parsePocketPickFromUrl(url, html)
        }
        if (url != null && (
                url.contains("whichchoice=${DemonInCombatNameSync.ALLIED_RADIO_BACKPACK_CHOICE}") ||
                url.contains("whichchoice=${DemonInCombatNameSync.ALLIED_RADIO_HANDHELD_CHOICE}")
            )
        ) {
            preferences?.let { AlliedRadioRequest.parseVisitChoice(html, it) }
            demonInCombatNameSync?.parseRadioResponse(html)
        }
        if (url != null && url.contains("desc_item.php")) {
            extractDescItemId(url)?.let { descId ->
                preferences?.let { ItemDescriptionConsequenceSync.applyItemDescription(descId, html, it) }
                val itemId = ItemDatabase.getByDescId(descId)?.id
                    ?: DescriptionCache.parseItemIdFromHtml(html)
                if (itemId != null && itemId > 0) {
                    DescriptionCache.cacheItem(itemId, html)
                    NemesisRequest.parsePaperStrip(itemId, html, preferences)
                    if (itemId == LocketRequest.LOCKET_ITEM_ID) {
                        LocketManager.parseLocket(html, preferences)
                    }
                }
                val item = ItemDatabase.getByDescId(descId)
                val char = character
                val state = char?.state?.value
                if (item != null && char != null && state != null && state.ascensionPath.canUseFamiliars()) {
                    val owned = familiarManager?.state?.value?.ownedFamiliars ?: emptyList()
                    when (item.id) {
                        CrownBjornDescSync.CROWN_ITEM_ID -> {
                            val occupant = CrownBjornDescSync.parseOccupant(html, owned)
                            char.updateEnthroned(occupant.id, occupant.race)
                        }
                        CrownBjornDescSync.BJORN_ITEM_ID -> {
                            val occupant = CrownBjornDescSync.parseOccupant(html, owned)
                            char.updateBjorned(occupant.id, occupant.race)
                        }
                    }
                }
            }
        }
        if (url != null && url.contains("desc_effect.php")) {
            extractDescEffectId(url)?.let { descId ->
                preferences?.let { EffectDescriptionConsequenceSync.applyEffectDescription(descId, html, it) }
                EffectDatabase.getByDescId(descId)?.id?.let { effectId ->
                    DescriptionCache.cacheEffect(effectId, html)
                }
            }
        }
        if (url != null && url.contains("desc_skill.php") && url.contains("self=true")) {
            extractDescSkillId(url)?.let { skillId ->
                DescriptionCache.cacheSkill(skillId, html)
                preferences?.let { prefs ->
                    SkillDescriptionConsequenceSync.applySkillDescription(skillId, html, prefs)
                    if (skillId == BirdOfTheDaySync.SEEK_OUT_A_BIRD_SKILL_ID) {
                        val newlyUnlocked = BirdOfTheDaySync.applySeekBirdSkillDescription(html, prefs)
                        if (newlyUnlocked) {
                            SkillLearner.learnSkill(
                                BirdOfTheDaySync.SEEK_OUT_A_BIRD_SKILL_ID,
                                prefs,
                                skillManager,
                                mpCostOverride = BirdOfTheDaySync.parseSkillMpCost(html).toInt(),
                                firstLearnOnly = true,
                            )
                            kotlinx.coroutines.runBlocking { skillManager?.fetchSkills() }
                        }
                    }
                }
            }
        }
        if (url != null && (
                url.contains("fight.php") ||
                    url.contains("fambattle.php") ||
                    html.contains("You're fighting")
            )
        ) {
            character?.let { ClassResourceCombatSync.apply(it, html) }
            character?.let { char ->
                FightPokefamSync.apply(char, html, familiarManager, preferences, sessionLogger)
            }
            preferences?.let { prefs ->
                val exprCtx = character?.state?.value?.let { state ->
                    ExpressionContext.from(state, emptyList())
                } ?: ExpressionContext.EMPTY
                CombatSkillConsequenceSync.applyFromFightHtml(html, prefs, exprCtx)
                // Dwarf war uniform combat deduce (Phases 2646–2660)
                if (html.contains("mattock glows", ignoreCase = true) ||
                    html.contains("crystal lens flips", ignoreCase = true) ||
                    html.contains("sporran", ignoreCase = true)
                ) {
                    DwarfFactoryRequest.deduceHP(html)
                    DwarfFactoryRequest.deduceAttack(html, prefs)
                    DwarfFactoryRequest.deduceDefense(html)
                }
                LatteChoiceSync.applyFight(
                    location = prefs.getString(Preferences.LAST_LOCATION, "").ifBlank { null },
                    html = html,
                    preferences = prefs,
                    sessionLog = { line -> sessionLogger?.appendRawLine(line) },
                )
                extractDescSkillId(url)?.let { skillId ->
                    LatteChoiceSync.applySkillCast(skillId, prefs)
                }
                val fight = AdventureParser.parseFightResult(html)
                if (fight.won) {
                    MonsterDatabase.getByName(fight.monster)?.id?.let { monsterId ->
                        GreyYouManager.absorbMonster(
                            monsterId,
                            html,
                            character?.state?.value?.ascensionPath == AscensionPath.GREY_YOU,
                            prefs,
                        )
                    }
                }
                SkillLearnFromResponse.learnSkillFromResponse(
                    html,
                    prefs,
                    skillManager,
                    inventoryManager,
                )
            }
            DreadScrollManager.handleKillscroll(html, preferences, sessionLogger)
            DreadScrollManager.handleHealscroll(html, preferences, sessionLogger)
            ElVibratoManager.parseResponse(
                url = url,
                html = html,
                consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
            )
            val charState = character?.state?.value
            TavernCellarSync.applyFromVisit(
                url = url,
                html = html,
                preferences = preferences,
                questDatabase = questDatabase,
                ascensionNumber = charState?.ascensionNumber ?: 0,
                shouldSkipExplore = { shouldSkipTavernExplore(charState) },
            )
            ProtonicGhostSync.applyFromFight(
                html = html,
                questDatabase = questDatabase,
                preferences = preferences,
                turnsPlayed = charState?.turnsPlayed ?: 0,
                equipment = charState?.equipment ?: emptyMap(),
            )
            QuestFightStartedSync.apply(
                monster = AdventureParser.parseEncounterMonsterName(html).orEmpty(),
                html = html,
                preferences = preferences,
                turnsPlayed = charState?.turnsPlayed ?: 0,
                equipment = charState?.equipment ?: emptyMap(),
                clearSlot = { slot -> character?.updateEquipment(slot, "") },
                consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                allowUnequippedConsume = !QuestFightStartedSync.isCombatActionUrl(url),
            )
            VoteMonsterManager.checkCounter(preferences, charState?.turnsPlayed ?: 0)
            CryptManager.handleFightEvilness(html, "", preferences)
            FireExtinguisherCombatSync.apply(html, "", preferences, questDatabase)
            val fightMonster = AdventureParser.parseEncounterMonsterName(html).orEmpty()
            BugbearManager.handleKeyotron(html, fightMonster, preferences)
            NewYouCombatSync.apply(html, questDatabase, preferences)
            val combatItemId = extractDescItemId(url)?.toIntOrNull()
            val lastMonster = fightMonster.ifBlank {
                preferences?.getString(Preferences.LAST_MONSTER, "").orEmpty()
            }
            FightItemPrefSync.apply(
                html = html,
                monster = lastMonster,
                preferences = preferences,
                combatItemId = combatItemId,
                consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                currentRun = character?.state?.value?.currentRun ?: 0,
            )
            FightStructuralSync.apply(
                FightStructuralSync.Context(
                    html = html,
                    location = preferences?.getString(Preferences.LAST_LOCATION, "").orEmpty(),
                    adventureId = url,
                    monsterName = lastMonster,
                    won = AdventureParser.parseFightResult(html).won,
                    preferences = preferences,
                    inventory = inventoryManager,
                    sessionLogger = sessionLogger,
                    clearEquipment = { slot -> character?.updateEquipment(slot, "") },
                ),
            )
            FightIotmResidualSync.apply(
                html = html,
                monsterName = lastMonster,
                preferences = preferences,
                itemCount = { id -> inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0 },
                daylightShavingsEquipped = character?.state?.value?.equipment?.values?.any {
                    it.contains("Daylight Shavings Helmet", ignoreCase = true)
                } == true,
                cursedMagnifyingGlassEquipped = character?.state?.value?.equipment?.values?.any {
                    it.contains("Cursed Magnifying Glass", ignoreCase = true)
                } == true,
                locationName = preferences?.getString(Preferences.LAST_LOCATION, ""),
                currentRun = character?.state?.value?.currentRun ?: 0,
                familiarHasStillSuit = StillSuitManager.hasStillSuit(
                    character?.state?.value?.equipment?.get(EquipmentSlot.FAMILIAR),
                ),
                anyOwnedFamiliarHasStillSuit = familiarManager?.state?.value?.ownedFamiliars.orEmpty()
                    .any { it.equipment?.itemId == ItemPool.STILLSUIT },
                crystalBallEquipped = CrystalBallManager.isEquipped(
                    character?.state?.value?.equipment?.get(EquipmentSlot.FAMILIAR),
                ),
            )
            WumpusManager.onWumpusFight(html)
            extractDescItemId(url)?.toIntOrNull()?.let { itemId ->
                QuestItemUsedSync.apply(
                    itemId,
                    html,
                    questDatabase,
                    preferences,
                    consumeItem = { id, qty -> inventoryManager?.consumeItemLocally(id, qty) },
                    count = extractUseQuantity(url),
                )
            }
        }
        if (url != null && url.contains("elvmachine.php", ignoreCase = true)) {
            ElvmachineRequest.parseResponse(
                url = url,
                html = html,
                sessionLogger = sessionLogger,
                consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
            )
        }
        if (url != null && url.contains("whichchoice=704", ignoreCase = true)) {
            DreadScrollManager.handleLibrary(html, preferences, sessionLogger)
        }
        if (url != null && url.contains("whichchoice=703", ignoreCase = true)) {
            if (html.contains(DreadScrollManager.HIGH_PRIEST_SUCCESS, ignoreCase = true)) {
                DreadScrollManager.handleHighPriestSuccess(html, preferences, eventBus, sessionLogger)
            } else {
                DreadScrollManager.recordFailure(url, html, preferences)
            }
        }
        if (url != null) {
            MerkinQuestSync.applyFromUrl(url, preferences, sessionLogger)
        }
        if (url != null && url.contains("sea_merkin.php", ignoreCase = true)) {
            SeaMerkinSync.parseTemple(
                url,
                html,
                character?.state?.value?.inSeaPath == true,
                preferences,
                sessionLogger,
            )
        }
        if (url != null && url.contains("adventure.php", ignoreCase = true)) {
            SeaMerkinSync.parseColosseum(url, html, preferences, sessionLogger)
        }
        if (url != null &&
            url.contains("inv_use.php", ignoreCase = true) &&
            url.contains("whichitem=${DreadScrollManager.DREADSCROLL_ID}")
        ) {
            DreadScrollManager.parseDreadscrollUse(html, preferences, eventBus, sessionLogger)
        }
        if (url != null &&
            url.contains("inv_use.php", ignoreCase = true) &&
            url.contains("whichitem=${DreadScrollManager.KNUCKLEBONE_ID}")
        ) {
            DreadScrollManager.handleKnucklebone(html, preferences, sessionLogger)
        }
        if (url != null &&
            url.contains("inv_use.php", ignoreCase = true) &&
            url.contains("whichitem=${ProtonicGhostSync.WALKIE_TALKIE}")
        ) {
            ProtonicGhostSync.applyFromWalkieTalkie(
                html = html,
                questDatabase = questDatabase,
                preferences = preferences,
                turnsPlayed = character?.state?.value?.turnsPlayed ?: 0,
            )
        }
        if (url != null && url.contains("craft.php", ignoreCase = true)) {
            craftRequest?.applyCraftResponse(url, html)
                ?: CreateItemCraftSync.parseCrafting(
                    location = url,
                    responseText = html,
                    inventory = inventoryManager,
                    preferences = preferences,
                    characterState = character?.state?.value,
                    sessionLogger = sessionLogger,
                )
        }
        if (url != null && url.contains("inventory.php", ignoreCase = true) &&
            url.contains("reminisce", ignoreCase = true)
        ) {
            LocketManager.parseMonsters(html, preferences)
        }
        if (url != null && (
            url.contains("inv_equip.php", ignoreCase = true) ||
                (url.contains("inventory.php", ignoreCase = true) &&
                    url.contains("action=holster", ignoreCase = true))
            )
        ) {
            EquipmentRequest.parseEquipmentChange(url, html, equipmentManager)
        }
        if (url != null && url.contains("inv_use.php", ignoreCase = true)) {
            extractDescItemId(url)?.toIntOrNull()?.let { itemId ->
                val qty = extractUseQuantity(url)
                val questHandled = QuestItemUsedSync.apply(
                    itemId,
                    html,
                    questDatabase,
                    preferences,
                    consumeItem = { id, q -> inventoryManager?.consumeItemLocally(id, q) },
                    count = qty,
                )
                UseItemConsumptionSync.rememberLastItem(itemId, qty)
                UseItemConsumptionSync.parseConsumption(
                    responseText = html,
                    itemId = itemId,
                    count = qty,
                    preferences = preferences,
                    character = character,
                    inventory = if (questHandled) null else inventoryManager,
                )
                preferences?.let { prefs ->
                    when (itemId) {
                        DwarfFactoryRequest.SMALL_LAMINATED_CARD,
                        DwarfFactoryRequest.LITTLE_LAMINATED_CARD,
                        DwarfFactoryRequest.NOTBIG_LAMINATED_CARD,
                        DwarfFactoryRequest.UNLARGE_LAMINATED_CARD,
                        -> DwarfFactoryRequest.useLaminatedItem(itemId, html, prefs)
                        DwarfFactoryRequest.DWARVISH_DOCUMENT,
                        DwarfFactoryRequest.DWARVISH_PAPER,
                        DwarfFactoryRequest.DWARVISH_PARCHMENT,
                        -> DwarfFactoryRequest.useUnlaminatedItem(itemId, html, prefs)
                    }
                }
            }
        }
        if (url != null && (
            url.contains("inv_eat.php", ignoreCase = true) ||
                url.contains("inv_booze.php", ignoreCase = true) ||
                url.contains("inv_spleen.php", ignoreCase = true)
            )
        ) {
            extractDescItemId(url)?.toIntOrNull()?.let { itemId ->
                val qty = extractUseQuantity(url)
                UseItemConsumptionSync.rememberLastItem(itemId, qty)
                UseItemConsumptionSync.parseConsumption(
                    responseText = html,
                    itemId = itemId,
                    count = qty,
                    preferences = preferences,
                    character = character,
                    inventory = inventoryManager,
                )
            }
        }
        CryptManager.applyAcquireFromHtml(html, questDatabase, preferences)
        CryptManager.applyFromVisit(url, html, preferences)
        if (url != null &&
            url.contains("skills.php", ignoreCase = true) &&
            url.contains("whichskill=${DreadScrollManager.DEEP_DARK_VISIONS_SKILL}")
        ) {
            DreadScrollManager.handleDeepDarkVisions(html, preferences, sessionLogger)
        }
        if (url != null && (
                url.contains("skills.php", ignoreCase = true) ||
                    url.contains("skillz.php", ignoreCase = true)
            ) && url.contains("whichskill=", ignoreCase = true)
        ) {
            net.sourceforge.kolmafia.skill.UseSkillSync.parseResponse(
                urlString = url,
                responseText = html,
                preferences = preferences,
                character = character,
            )
        }
        if (url != null) {
            when {
                url.contains("clan_viplounge.php", true) ->
                    ClanLoungeRequest.parseResponse(url, html, preferences)
                url.contains("clan_rumpus.php", true) ->
                    ClanRumpusRequest.parseResponse(url, html, preferences)
                url.contains("clan_hall.php", true) -> ClanHallRequest.parseResponse(url, html)
                url.contains("showclan.php", true) ||
                    url.contains("clan_members.php", true) ||
                    url.contains("clan_detailedroster.php", true) ->
                    ClanMembersRequest.parseResponse(url, html)
                url.contains("clan_log.php", true) -> ClanLogRequest.parseResponse(url, html)
                url.contains("clan_attack.php", true) || url.contains("clan_war.php", true) ->
                    ClanWarRequest.parseResponse(url, html, preferences)
            }
        }
        ResponseTextParser.externalUpdate(
            url = url,
            html = html,
            preferences = preferences,
            character = character,
            inventory = inventoryManager,
        )
        if (url != null && (
                url.contains("charpane.php", ignoreCase = true) ||
                url.endsWith("/charpane.php", ignoreCase = true)
            )
        ) {
            character?.let { char ->
                CharpaneStatusSync.apply(char, html, preferences, familiarManager)
                ClassResourceCharpaneSync.apply(char, html)
            }
            ClanIdSync.apply(html)
        }
        if (url != null && (
                url.contains("api.php", ignoreCase = true) &&
                    url.contains("what=status", ignoreCase = true)
            )
        ) {
            SpelunkyRequest.parseStatus(html, preferences)
            character?.let { char ->
                ApiStatusSync.parseStatus(
                    responseText = html,
                    character = char,
                    preferences = preferences,
                    effectManager = effectManager,
                    equipmentManager = equipmentManager,
                    familiarManager = familiarManager,
                )
            }
        }
        if (url != null && url.contains("campground.php", ignoreCase = true)) {
            CampgroundSync.parseResponse(
                url = url,
                html = html,
                preferences = preferences,
                character = character,
                inventory = inventoryManager,
            )
            preferences?.let { prefs ->
                PortalRequest.parseResponse(
                    url = url,
                    html = html,
                    preferences = prefs,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                )
            }
        }
        if (url != null && url.contains("closet.php", ignoreCase = true)) {
            character?.let { ClosetMeatSync.apply(it, html, url) }
        }
        if (url != null && url.contains("storage.php", ignoreCase = true)) {
            character?.let { StorageMeatSync.apply(it, html, url) }
        }
        if (url != null && url.contains("shop.php", ignoreCase = true)) {
            character?.let { StillSync.apply(it, html, url) }
            val visitState = character?.state?.value
            ShopInventorySync.parseAndLearn(
                html = html,
                url = url,
                sessionLogger = sessionLogger,
                prefs = preferences,
                state = visitState,
            ) { skillId ->
                fetchDescription("desc_skill.php?whichskill=$skillId&self=true")
            }
            preferences?.let { prefs ->
                val state = visitState
                val ascension = state?.ascensionNumber ?: 0
                NpcShopSync.applyShopVisit(html, url, prefs, ascension)
            }
        }
        if (url != null && url.contains("store.php", ignoreCase = true)) {
            preferences?.let { prefs ->
                val ascension = character?.state?.value?.ascensionNumber ?: 0
                NpcShopSync.applyShopVisit(html, url, prefs, ascension)
            }
        }
        if (url != null && url.contains("peevpee.php", ignoreCase = true)) {
            PeeVPeeRequest.registerRequest(url, sessionLogger)
            PeeVPeeSync.apply(html, url, character, preferences, sessionLogger, inventoryManager)
            if (url.contains("place=shop", ignoreCase = true)) {
                preferences?.let {
                    SwaggerShopSync.applyVisitShop(html, url, it, sessionLogger, character?.state?.value)
                }
            }
        }
        if (url != null && url.contains("showplayer.php", ignoreCase = true)) {
            ProfileRequest.applyFromVisit(html, url, character)
        }
        if (url != null && url.contains("place.php", ignoreCase = true)) {
            PlaceSync.parseResponse(
                url = url,
                html = html,
                preferences = preferences,
                character = character,
                inventory = inventoryManager,
            )
            ShadowRiftSync.applyIngressFromUrl(url, preferences)
            if (url.contains("whichplace=arcade", ignoreCase = true)) {
                preferences?.let { prefs ->
                    ArcadeRequest.parseResponse(url, html, prefs, ResultProcessor)
                    ArcadeRequest.registerRequest(
                        urlString = url,
                        sessionLogger = sessionLogger,
                        preferences = prefs,
                        ascensions = character?.state?.value?.ascensionNumber ?: 0,
                        tokenCount = inventoryManager?.state?.value?.items
                            ?.get(ArcadeRequest.GG_TOKEN)?.quantity ?: 0,
                    )
                }
            }
        }
        if (url != null && url.contains("dwarffactory.php", ignoreCase = true)) {
            preferences?.let { prefs ->
                val asc = character?.state?.value?.ascensionNumber ?: 0
                DwarfFactoryRequest.ensureUpdated(prefs, asc)
                DwarfFactoryRequest.parseResponse(url, html, prefs, asc, sessionLogger)
                DwarfFactoryRequest.registerRequest(url, sessionLogger)
            }
        }
        if (url != null && url.contains("dwarfcontraption.php", ignoreCase = true)) {
            preferences?.let { prefs ->
                val asc = character?.state?.value?.ascensionNumber ?: 0
                DwarfContraptionRequest.parseResponse(
                    urlString = url,
                    responseText = html,
                    preferences = prefs,
                    inventoryManager = inventoryManager,
                    resultProcessor = ResultProcessor,
                    ascensions = asc,
                )
                DwarfContraptionRequest.registerRequest(url, sessionLogger)
            }
        }
        if (url != null && url.contains("basement.php", ignoreCase = true)) {
            val state = character?.state?.value
            BasementSync.checkBasement(
                html = html,
                preferences = preferences,
                autoSwitch = false,
                muscle = state?.buffedMusc ?: 0,
                mysticality = state?.buffedMyst ?: 0,
                moxie = state?.buffedMoxie ?: 0,
                maxHp = state?.maxHp ?: 0,
                maxMp = state?.maxMp ?: 0,
            )
        }
        if (url != null && url.contains("place.php", ignoreCase = true) &&
            url.contains("place=twitch", ignoreCase = true)
        ) {
            preferences?.let { TimeTowerSync.syncFromTwitchPlaceHtml(html, it) }
        }
        if (url != null && url.contains("place.php", ignoreCase = true) &&
            url.contains("whichplace=crimbo23", ignoreCase = true)
        ) {
            preferences?.let { Crimbo23ZoneSync.syncFromPlaceHtml(html, it) }
        }
        if (url != null && url.contains("place.php", ignoreCase = true) &&
            url.contains("whichplace=spelunky", ignoreCase = true)
        ) {
            SpelunkyRequest.parseResponse(url, html, preferences, sessionLogger)
        }
        if (url != null && url.contains("place.php", ignoreCase = true) &&
            url.contains("batman_", ignoreCase = true)
        ) {
            BatFellowRequest.parseResponse(url, html, preferences)
            BatFellowRequest.registerRequest(url, preferences, sessionLogger)
        }
        if (url != null && (
                url.contains("adventure.php", ignoreCase = true) ||
                url.contains("place.php", ignoreCase = true) ||
                url.contains("choice.php", ignoreCase = true)
            )
        ) {
            preferences?.let { prefs ->
                if (url.contains("adventure.php", ignoreCase = true) ||
                    url.contains("place.php", ignoreCase = true)
                ) {
                    AirportSync.syncFromVisit(
                        html = html,
                        url = url,
                        prefs = prefs,
                        consumeItem = { itemId -> inventoryManager?.consumeItemLocally(itemId, 1) },
                    )
                    GingerbreadCitySync.applyFromVisit(url, html, prefs)
                    SpacegateVisitSync.applyFromVisit(url, html, prefs)
                    SpacegateAdventureSync.applyFromAdventure(url, html, prefs)
                }
                SpacegateTerminalSync.applyFromTerminal(url, html, prefs)
            }
        }
        if (url != null && url.contains("knoll_mushrooms.php", ignoreCase = true)) {
            character?.let {
                MushroomManager.parsePlot(html, preferences, it, url)
            }
        }
        if (url != null && url.contains("sushi.php", ignoreCase = true)) {
            SushiConsumptionSync.parseConsumptionFromVisit(
                url = url,
                responseText = html,
                character = character,
                eventBus = eventBus,
                preferences = preferences,
            )
        }
        if (url != null && url.contains("barrelshrine=1", ignoreCase = true)) {
            preferences?.let { BarrelShrineSync.syncUnlockFromHtml(html, it) }
        }
        if (html.contains("barrelshrine", ignoreCase = true)) {
            preferences?.let { BarrelShrineSync.syncUnlockFromHtml(html, it) }
        }
        if (preferences != null && (
                url?.contains("whichchoice=1100", ignoreCase = true) == true ||
                    ChoiceUtilities.extractChoiceId(html) == BarrelChoiceMapper.CHOICE_ID
                )
        ) {
            BarrelShrineSync.syncFromVisit(html, preferences)
        }
        run {
            val choiceId = url?.let { WHICH_CHOICE_URL_PATTERN.find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
                ?: ChoiceUtilities.extractChoiceId(html)
            if (choiceId != null && preferences != null) {
                ShenSync.applyVisitChoice(choiceId, html, preferences)
                HiddenCityChoiceSync.applyVisitChoice(choiceId, html, preferences)
                PartyFairChoiceSync.applyVisit(choiceId, html, preferences)
                LightsOutChoiceSync.applyVisit(
                    choiceId,
                    preferences,
                    character?.state?.value?.turnsPlayed ?: 0,
                )
                SnojoChoiceSync.applyVisit(choiceId, html, preferences)
                SpoopyChoiceSync.applyVisit(choiceId, html, preferences)
                VillainLairChoiceSync.applyVisit(choiceId, html, preferences)
                MonorailChoiceSync.applyVisit(
                    choiceId,
                    html,
                    preferences,
                ) { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) }
                SpacegateVaccinatorChoiceSync.applyVisit(choiceId, html, preferences)
                FloristFriarChoiceSync.apply(choiceId, url.orEmpty(), html, preferences)
                WlfBunkerChoiceSync.applyVisit(
                    choiceId,
                    html,
                    preferences,
                ) { descId -> ItemDatabase.getByDescId(descId)?.id }
                SpacegateLeftoversChoiceSync.applyVisit(choiceId, html) {
                    sessionLogger?.appendRawLine(it)
                }
                TrickOrTreatChoiceSync.applyVisit(choiceId, html, preferences)
                ArchSpadeChoiceSync.applyVisit(choiceId, html, preferences)
                DeckChoiceSync.applyVisit(choiceId, html, preferences)
                AutomatedFutureChoiceSync.applyVisit(choiceId, html, preferences)
                MobiusChoiceSync.applyVisit(
                    choiceId,
                    preferences,
                    character?.state?.value?.turnsPlayed ?: 0,
                )
                BaseballChoiceSync.applyVisit(choiceId, html, preferences)
                MushyCenterChoiceSync.applyVisit(choiceId, html, preferences)
                HorseryChoiceSync.applyVisit(choiceId, html, preferences)
                MimicDnaChoiceSync.applyVisit(choiceId, html, preferences)
                StalagmiteChoiceSync.applyVisit(choiceId, preferences)
                PowerPlantChoiceSync.applyVisit(choiceId, html, preferences)
                ColdMedicineChoiceSync.applyVisit(choiceId, html, preferences)
                PlumberShopChoiceSync.applyVisit(choiceId, html, preferences)
                BackupCameraChoiceSync.applyVisit(choiceId, html, preferences)
                CrystalBallChoiceSync.applyVisit(
                    choiceId,
                    html,
                    preferences,
                    currentRun = character?.state?.value?.currentRun ?: 0,
                )
                AutumnatonChoiceSync.applyVisit(choiceId, html, preferences)
                TrainsetChoiceSync.applyVisit(choiceId, html, preferences)
                JuneCleaverChoiceSync.apply(
                    choiceId = choiceId,
                    decision = 0,
                    preferences = preferences,
                    choiceUrl = url.orEmpty(),
                )
                if (choiceId == LocketRequest.CHOICE_ID &&
                    url?.contains("option=1", ignoreCase = true) == true
                ) {
                    Regex("""(?:^|[?&])mid=(\d+)""", RegexOption.IGNORE_CASE)
                        .find(url)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.toIntOrNull()
                        ?.let { LocketRequest.recordReminisce(preferences, it) }
                }
                BurningLeavesChoiceSync.applyVisit(choiceId, html, preferences)
                YouRobotChoiceSync.applyVisit(choiceId, html, preferences, url.orEmpty(), skillManager)
                GrimstoneManager.applyVisit(choiceId, html, preferences)
                MayamChoiceSync.applyVisit(choiceId, html, preferences)
                TakerSpaceChoiceSync.applyVisit(choiceId, html, preferences) {
                    ConcoctionDatabase.refreshConcoctionsNowFromLastContext()
                }
                SpecimenBenchChoiceSync.applyVisit(choiceId, html, preferences)
                LeprecondoChoiceSync.applyVisit(choiceId, html, preferences)
                HaciendaChoiceSync.applyVisit(choiceId, html, preferences)
                PerilChoiceSync.applyVisit(choiceId, html, preferences)
                PeridotChoiceSync.applyVisit(
                    choiceId = choiceId,
                    preferences = preferences,
                    lastVisitedLocationName = preferences?.getString(
                        Preferences.LAST_LOCATION,
                        "",
                    ).orEmpty(),
                )
                CrimboPastChoiceSync.applyVisit(choiceId, html, preferences)
                MonkeyPawChoiceSync.applyVisit(choiceId, html, preferences)
                CoolerYetiChoiceSync.applyVisit(choiceId, html, preferences)
                CartographyChoiceSync.applyVisit(
                    choiceId = choiceId,
                    preferences = preferences,
                    ascensionNumber = character?.state?.value?.ascensionNumber ?: 0,
                )
                SausageGrinderChoiceSync.applyVisit(choiceId, html, preferences)
                BoomBoxChoiceSync.applyVisit(choiceId, html, preferences)
                RedSnapperChoiceSync.applyVisit(
                    choiceId = choiceId,
                    html = html,
                    preferences = preferences,
                    currentTurn = character?.state?.value?.currentRun ?: 0,
                )
                DoctorBagChoiceSync.applyVisit(choiceId, html, preferences)
                VoteBallotChoiceSync.applyVisit(choiceId, html, preferences)
                LatteChoiceSync.applyVisit(choiceId, html, preferences)
                MotorbikeChoiceSync.applyVisit(choiceId, html, preferences)
                GenieChoiceSync.applyVisit(choiceId, html, preferences)
                DetectiveCaseSync.applyVisit(choiceId, html, preferences)
                ControlPanelChoiceSync.applyVisit(
                    choiceId = choiceId,
                    html = html,
                    preferences = preferences,
                    questDatabase = questDatabase,
                )
                IceHouseChoiceSync.applyVisit(
                    choiceId = choiceId,
                    html = html,
                    banishManager = banishManager,
                    currentTurn = character?.state?.value?.currentRun ?: 0,
                )
                DaycareChoiceSync.applyVisit(choiceId, html, preferences)
                CyberRealmSync.applyFromChoice(choiceId, preferences)
            }
        }
        if (url != null && url.contains("guild.php", ignoreCase = true)) {
            GuildVisitSync.syncStoreOpen(html, character, preferences)
            GuildVisitSync.parseFromVisit(
                url,
                html,
                eventBus,
                sessionLogger,
                character,
                preferences,
                skillManager,
                inventoryManager,
                questDatabase,
            )
        }
        ClanLoungeSync.apply(preferences, html, url)
        // Track K syncs (Phase 960–963, 967)
        if (url != null && preferences != null) {
            if (url.contains("clan_viplounge.php", ignoreCase = true)) {
                ClanLoungeVisitSync.parseAndWrite(html, preferences!!)
            }
            if (url.contains("clan_rumpus.php", ignoreCase = true)) {
                ClanRumpusVisitSync.parseAndWrite(html, preferences!!)
            }
            if (url.contains("whichplace=chateau", ignoreCase = true)) {
                ChateauVisitSync.parseAndWrite(html, preferences!!)
            }
            if (url.contains("managestore.php", ignoreCase = true)) {
                ShopInventoryVisitSync.parseAndWrite(html, preferences!!)
                StoreManager.update(html, StoreManager.TableType.ADDER)
            }
            if (url.contains("manageprices.php", ignoreCase = true)) {
                StoreManager.update(html, StoreManager.TableType.PRICER)
            }
            if (url.contains("backoffice.php", ignoreCase = true) && !url.contains("action=", ignoreCase = true)) {
                StoreManager.update(html, StoreManager.TableType.DEETS)
            }
        }
        // Track L sync (Phase 969) + FamiliarSync hub (Phases 2421–2450)
        if (url != null && url.contains("familiar.php", ignoreCase = true)) {
            FamiliarSync.parseResponse(
                url = url,
                html = html,
                familiarManager = familiarManager,
                preferences = preferences,
                character = character,
                equipmentManager = equipmentManager,
            )
            preferences?.let { FamiliarEquipmentLockSync.parseAndWrite(html, it) }
        }
        character?.let { SessionMeatSync.apply(it, html) }
        character?.state?.value?.let { state ->
            DispensarySync.applyFromResponse(html, state, preferences)
        }
        if (!questHubRouted) {
        if (url != null && (
                url.contains("whichplace=manor", ignoreCase = true) ||
                    url.contains("snarfblat=${SpookyravenManorVisitSync.HAUNTED_BILLIARDS_ROOM}") ||
                    url.contains("snarfblat=${SpookyravenManorVisitSync.HAUNTED_BALLROOM}") ||
                    (url.contains("manor", ignoreCase = true) && !url.contains("whichplace=", ignoreCase = true))
            )
        ) {
            preferences?.let { prefs ->
                questDatabase?.let { db ->
                    SpookyravenManorVisitSync.applyFromVisit(
                        url = url,
                        html = html,
                        questDatabase = db,
                        preferences = prefs,
                        context = SpookyravenManorVisitSync.ManorVisitContext(
                            ascensionNumber = character?.state?.value?.ascensionNumber ?: 0,
                            hasItemId = { id ->
                                inventoryManager?.state?.value?.items?.containsKey(id) == true
                            },
                            consumeItem = { itemId, quantity ->
                                inventoryManager?.consumeItemLocally(itemId, quantity)
                            },
                        ),
                    )
                }
            }
        }
        if (url != null && (
                url.contains("whichplace=desertbeach", ignoreCase = true) ||
                    url.contains("whichplace=exploathing_beach", ignoreCase = true)
            )
        ) {
            preferences?.let { prefs ->
                DesertVisitSync.applyFromVisit(
                    url = url,
                    html = html,
                    questDatabase = questDatabase,
                    preferences = prefs,
                )
            }
        }
        if (url != null && (
                url.contains("whichplace=pyramid", ignoreCase = true) ||
                    url.contains("action=db_pyramid1", ignoreCase = true) ||
                    url.contains("action=expl_pyramidpre", ignoreCase = true) ||
                    url.contains("action=pyramid_state", ignoreCase = true) ||
                    url.contains("snarfblat=${PyramidVisitSync.UPPER_CHAMBER}") ||
                    url.contains("snarfblat=${PyramidVisitSync.MIDDLE_CHAMBER}")
            )
        ) {
            preferences?.let { prefs ->
                questDatabase?.let { db ->
                    PyramidVisitSync.applyFromVisit(
                        url = url,
                        html = html,
                        questDatabase = db,
                        preferences = prefs,
                        context = PyramidVisitSync.PyramidVisitContext(
                            consumeItem = { itemId, quantity ->
                                inventoryManager?.consumeItemLocally(itemId, quantity)
                            },
                        ),
                    )
                }
            }
        }
        if (url != null && (
                url.contains("whichplace=palindome", ignoreCase = true) ||
                    url.contains("action=pal_mr", ignoreCase = true) ||
                    url.contains("snarfblat=${PalindomeSync.PALINDOME_ADVENTURE}")
            )
        ) {
            preferences?.let { prefs ->
                questDatabase?.let { db ->
                    PalindomeSync.applyFromVisit(
                        url = url,
                        html = html,
                        questDatabase = db,
                        preferences = prefs,
                        context = PalindomeSync.PalindomeVisitContext(
                            consumeItem = { itemId, quantity ->
                                inventoryManager?.consumeItemLocally(itemId, quantity)
                            },
                        ),
                    )
                }
            }
        }
        if (url != null && (
                url.contains("woods.php", ignoreCase = true) ||
                    url.contains("whichplace=woods", ignoreCase = true)
            )
        ) {
            preferences?.let { prefs ->
                BlackForestSync.applyWoodsVisit(
                    url = url,
                    html = html,
                    questDatabase = questDatabase,
                    preferences = prefs,
                    ascensionNumber = character?.state?.value?.ascensionNumber ?: 0,
                    consumeItem = { itemId ->
                        inventoryManager?.consumeItemLocally(itemId, 1)
                    },
                )
            }
        }
        if (url != null && (
                url.contains("whichplace=hiddencity", ignoreCase = true) ||
                    url.contains("whichshop=hiddentavern", ignoreCase = true) ||
                    html.contains("snarfblat=341") ||
                    html.contains("snarfblat=342") ||
                    html.contains("snarfblat=343") ||
                    html.contains("snarfblat=344")
            )
        ) {
            preferences?.let { prefs ->
                HiddenCityVisitSync.applyFromVisit(
                    url = url,
                    html = html,
                    preferences = prefs,
                    ascensionNumber = character?.state?.value?.ascensionNumber ?: 0,
                )
            }
        }
        if (url != null && url.contains("adventure.php", ignoreCase = true)) {
            val charState = character?.state?.value
            SneakyPeteDiscardSync.applyFromAdventure(
                html = html,
                inebriety = charState?.inebriety ?: 0,
                equipment = charState?.equipment ?: emptyMap(),
                clearSlot = { slot -> character?.updateEquipment(slot, "") },
                consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
            )
            ToppingPeakNcSync.applyFromAdventure(
                url = url,
                html = html,
                preferences = preferences,
            )
            preferences?.let { prefs ->
                val asc = character?.state?.value?.ascensionNumber ?: 0
                GarbageBeanstalkSync.applyFromAdventure(
                    url = url,
                    html = html,
                    questDatabase = questDatabase,
                    preferences = prefs,
                    ascensionNumber = asc,
                )
                ZeppelinRonSync.applyFromAdventure(
                    url = url,
                    html = html,
                    questDatabase = questDatabase,
                    preferences = prefs,
                    won = true,
                )
                WhiteCitadelSync.applyFromAdventure(
                    adventureId = null,
                    html = html,
                    questDatabase = questDatabase,
                    url = url,
                )
                ClancyNcSync.applyFromAdventure(null, html, questDatabase, url)
                SeaVisitSync.applyFromAdventure(null, html, questDatabase, url)
                TowerRuinsSync.applyFromAdventure(null, html, questDatabase, url)
                ExtremeSlopeSync.applyFromAdventure(null, html, prefs, url)
                PirateNcSync.applyFromAdventure(null, html, questDatabase, prefs, url)
                FarmDuckSync.applyFromAdventure(null, html, prefs, url)
                ElVibratoSync.applyFromAdventure(null, prefs, url)
                FriarsQuestSync.applyFromAdventure(
                    adventureId = null,
                    html = html,
                    preferences = prefs,
                    getTurns = { name -> adventureSpentTracker?.getTurns(name) ?: 0 },
                    url = url,
                )
                CyberRealmSync.applyFromAdventure(null, html, prefs, url)
                // Non-combat black forest progress texts also arrive on adventure.php
                if (url.contains("snarfblat=${BlackForestSync.BLACK_FOREST}")) {
                    questDatabase?.let { db ->
                        BlackForestSync.applyCombatWin(
                            questDatabase = db,
                            preferences = prefs,
                            adventureId = BlackForestSync.BLACK_FOREST.toString(),
                            responseText = html,
                            won = true,
                        )
                    }
                }
            }
        }
        if (url != null && url.contains("friars.php", ignoreCase = true)) {
            FriarsQuestSync.applyCeremony(
                url = url,
                html = html,
                questDatabase = questDatabase,
                preferences = preferences,
                consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
            )
        }
        if (url != null && url.contains("whichplace=realm_fantasy", ignoreCase = true)) {
            preferences?.let { FantasyRealmSync.applyFromFantasyPlace(url, html, it) }
        }
        if (url != null && url.contains("whichplace=monorail", ignoreCase = true)) {
            preferences?.let { prefs ->
                FantasyRealmSync.applyFromMonorail(url, html, prefs)
                CyberRealmSync.applyFromMonorail(url, html, prefs)
            }
        }
        if (url != null && (
                url.contains("whichplace=canadia", ignoreCase = true) ||
                    url.contains("action=lc_marty", ignoreCase = true)
            )
        ) {
            SwampQuestSync.applyFromCanadia(url, html, questDatabase)
        }
        if (url != null && url.contains("whichplace=marais", ignoreCase = true)) {
            preferences?.let { SwampQuestSync.applyFromMarais(url, html, questDatabase, it) }
        }
        if (url != null && (
                url.contains("whichplace=serverroom", ignoreCase = true) ||
                    url.contains("action=serverroom", ignoreCase = true)
            )
        ) {
            preferences?.let { CyberRealmSync.applyFromServerRoom(url, html, it) }
        }
        if (url != null && url.contains("whichplace=town", ignoreCase = true)) {
            preferences?.let { prefs ->
                val inBadMoon = ZodiacSign.find(character?.state?.value?.zodiacSign.orEmpty())?.isBadMoon == true
                TownUnlockSync.applyFromTownRight(url, html, prefs, inBadMoon)
                TownUnlockSync.applyFromTownWrong(url, html, prefs, inBadMoon)
                TownUnlockSync.applyFromTownMarket(url, html, prefs, inBadMoon)
                TownUnlockSync.applyFromTown(url, html, prefs)
                if (url.contains("action=townright_vote", ignoreCase = true)) {
                    VoteMonsterManager.applyFromVisit(url, html, prefs)
                }
            }
        }
        if (url != null && url.contains("speakeasy", ignoreCase = true)) {
            preferences?.let { TownUnlockSync.applyFromSpeakeasy(url, html, it) }
        }
        if (url != null && (
                url.contains("whichplace=orc_chasm", ignoreCase = true) ||
                    url.contains("whichplace=highlands", ignoreCase = true)
            )
        ) {
            preferences?.let { prefs ->
                ToppingPlaceSync.applyFromChasm(
                    url = url,
                    html = html,
                    questDatabase = questDatabase,
                    itemCount = { id -> inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0 },
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                )
                ToppingPlaceSync.applyFromHighlands(url, html, questDatabase, prefs)
            }
        }
        if (url != null && url.contains("bathole", ignoreCase = true)) {
            BatholeSync.applyFromVisit(url, html, questDatabase)
        }
        if (url != null && (
                url.contains("whichplace=sea_oldman", ignoreCase = true) ||
                    url.contains("monkeycastle.php", ignoreCase = true) ||
                    url.contains("seafloor.php", ignoreCase = true) ||
                    url.contains("sea_merkin.php", ignoreCase = true) ||
                    url.contains("action=oldman_oldman", ignoreCase = true) ||
                    url.contains("action=grandpastory", ignoreCase = true)
            )
        ) {
            val cls = character?.state?.value?.characterClassEnum
            SeaVisitSync.applyFromVisit(
                url = url,
                html = html,
                questDatabase = questDatabase,
                preferences = preferences,
                isMuscleClass = cls?.isMuscleBased == true,
                isMysticalityClass = cls?.isMysticality == true,
                isMoxieClass = cls?.isMoxieBased == true,
            )
        }
        if (url != null && url.contains("whichplace=plains", ignoreCase = true)) {
            PlainsVisitSync.applyFromVisit(
                url = url,
                html = html,
                questDatabase = questDatabase,
                consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
            )
        }
        if (url != null && (
                url.contains("whichplace=beanstalk", ignoreCase = true) ||
                    url.contains("place=beanstalk", ignoreCase = true)
            )
        ) {
            GarbageBeanstalkSync.applyFromPlace(url, html, questDatabase)
        }
        if (url != null && url.contains("trickortreat", ignoreCase = true)) {
            val charState = character?.state?.value
            TrickOrTreatSync.applyFromVisit(
                url = url,
                html = html,
                equipment = charState?.equipment ?: emptyMap(),
                clearSlot = { slot -> character?.updateEquipment(slot, "") },
                consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
            )
        }
        if (url != null && url.contains("pandamonium.php", ignoreCase = true)) {
            PandamoniumVisitSync.applyFromVisit(url, questDatabase)
        }
        if (url != null && CouncilVisitSync.isCouncilUrl(url)) {
            CouncilVisitSync.applyFromVisit(
                url = url,
                html = html,
                questDatabase = questDatabase,
                preferences = preferences,
                level = character?.state?.value?.level ?: 1,
                consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
            )
        }
        if (url != null && url.contains("main.php", ignoreCase = true)) {
            IslandUnlockSync.applyFromMain(
                url = url,
                html = html,
                preferences = preferences,
                ascensionNumber = character?.state?.value?.ascensionNumber ?: 0,
            )
        }
        if (url != null && url.contains("fernruin", ignoreCase = true)) {
            FernruinVisitSync.applyFromVisit(url, questDatabase)
        }
        if (url != null && url.contains("tavern.php", ignoreCase = true)) {
            TavernVisitSync.applyFromVisit(url, html, questDatabase)
        }
        if (url != null && (
                url.contains("cellar.php", ignoreCase = true) ||
                    url.contains("choice.php", ignoreCase = true)
            )
        ) {
            val charState = character?.state?.value
            TavernCellarSync.applyFromVisit(
                url = url,
                html = html,
                preferences = preferences,
                questDatabase = questDatabase,
                ascensionNumber = charState?.ascensionNumber ?: 0,
                shouldSkipExplore = { shouldSkipTavernExplore(charState) },
            )
        }
        if (url != null && url.contains("wham.php", ignoreCase = true)) {
            DetectiveCaseSync.applyFromVisit(url, html, preferences)
        }
        if (url != null && url.contains("whichplace=mountains", ignoreCase = true)) {
            MelvinShirtSync.applyFromVisit(
                url = url,
                html = html,
                questDatabase = questDatabase,
                consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
            )
        }
        if (url != null && url.contains("cell37", ignoreCase = true)) {
            Cell37EscapeSync.applyFromVisit(
                url = url,
                html = html,
                questDatabase = questDatabase,
                itemCount = { id -> inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0 },
                consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
            )
        }
        if (url != null && (
                url.contains("whichplace=mclargehuge", ignoreCase = true) ||
                    url.contains("cloudypeak", ignoreCase = true)
            )
        ) {
            preferences?.let { prefs ->
                ExtremeSlopeSync.applyCloudyPeak(url, html, questDatabase, prefs)
                TrapperCabinSync.applyFromVisit(
                    url = url,
                    html = html,
                    questDatabase = questDatabase,
                    preferences = prefs,
                    ascensionNumber = character?.state?.value?.ascensionNumber ?: 0,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                )
            }
        }
        }
        if (url?.contains("bigisland.php", ignoreCase = true) == true) {
            preferences?.let { prefs ->
                val equipment = character?.state?.value?.equipment ?: emptyMap()
                val islandVisitContext = IslandWarVisitSync.IslandVisitContext(
                    hasItemId = { id ->
                        inventoryManager?.state?.value?.items?.containsKey(id) == true
                    },
                    consumeItem = { itemId, quantity ->
                        inventoryManager?.consumeItemLocally(itemId, quantity)
                    },
                    isWearingWarHippyOutfit = {
                        val outfit = OutfitDatabase.getById(OutfitPool.WAR_HIPPY_OUTFIT)
                            ?: return@IslandVisitContext false
                        OutfitManager.isWearingPieces(outfit.equipment, equipment)
                    },
                    ascensionNumber = character?.state?.value?.ascensionNumber ?: 0,
                    itemCount = { id ->
                        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
                    },
                )
                IslandWarVisitLogSync.register(
                    url = url,
                    html = html,
                    preferences = prefs,
                    context = islandVisitContext,
                    sessionLogger = sessionLogger,
                )
                IslandWarVisitSync.applyFromBigIslandVisit(
                    url = url,
                    html = html,
                    preferences = prefs,
                    sessionLogger = sessionLogger,
                    context = islandVisitContext,
                )
                IslandWarActionResponseSync.parseActionResponse(
                    url = url,
                    html = html,
                    preferences = prefs,
                    context = islandVisitContext,
                )
            }
        }
        if (url?.contains("postwarisland.php", ignoreCase = true) == true) {
            preferences?.let { prefs ->
                val equipment = character?.state?.value?.equipment ?: emptyMap()
                val islandVisitContext = IslandWarVisitSync.IslandVisitContext(
                    hasItemId = { id ->
                        inventoryManager?.state?.value?.items?.containsKey(id) == true
                    },
                    consumeItem = { itemId, quantity ->
                        inventoryManager?.consumeItemLocally(itemId, quantity)
                    },
                    isWearingWarHippyOutfit = {
                        val outfit = OutfitDatabase.getById(OutfitPool.WAR_HIPPY_OUTFIT)
                            ?: return@IslandVisitContext false
                        OutfitManager.isWearingPieces(outfit.equipment, equipment)
                    },
                    ascensionNumber = character?.state?.value?.ascensionNumber ?: 0,
                    itemCount = { id ->
                        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
                    },
                )
                IslandWarVisitLogSync.register(
                    url = url,
                    html = html,
                    preferences = prefs,
                    context = islandVisitContext,
                    sessionLogger = sessionLogger,
                )
                IslandWarVisitSync.applyFromPostwarIslandVisit(
                    url = url,
                    html = html,
                    preferences = prefs,
                    context = islandVisitContext,
                )
                IslandWarActionResponseSync.parseActionResponse(
                    url = url,
                    html = html,
                    preferences = prefs,
                    context = islandVisitContext,
                )
            }
        }
    }

    /**
     * Normalized residual-request dispatcher. Request-specific parsers are added
     * here so typed requests and generic visit wrappers share one response path.
     */
    internal fun processVisitResponseHooksForPath(
        normalizedUrl: String,
        html: String,
        choiceId: Int?,
    ) {
        if (normalizedUrl.isBlank()) return
        val signature = normalizedUrl to html
        if (signature in handledResidualResponseSignatures) return

        val handled = when (choiceId) {
            TeaTreeChoiceSync.TREE_TEA,
            TeaTreeChoiceSync.SPECIFICI_TEA,
            -> if (isTeaTreeSuccess(html)) {
                TeaTreeChoiceSync.apply(
                    choiceId = choiceId,
                    decision = extractChoiceDecision(normalizedUrl),
                    preferences = preferences,
                    choiceUrl = normalizedUrl,
                    html = html,
                )
            } else {
                false
            }
            HashingChoiceSync.CHOICE_ID -> HashingChoiceSync.apply(
                choiceId = choiceId,
                html = html,
                choiceUrl = normalizedUrl,
                consumeItem = { itemId, qty ->
                    inventoryManager?.consumeItemLocally(itemId, qty)
                },
            )
            else -> if (KgbRequest.isKgbUrl(normalizedUrl)) {
                KgbRequest.parseResponse(normalizedUrl, html, preferences) {
                    checkDynamicModifiers()
                }
            } else if (PizzaCubeRequest.isPizzaUrl(normalizedUrl)) {
                PizzaCubeRequest.parseResponse(
                    normalizedUrl,
                    html,
                    inventoryManager,
                    preferences,
                )
            } else {
                false
            }
        }
        if (handled) {
            handledResidualResponseSignatures += signature
        }
    }

    /** Start a new request boundary where an identical response may be handled again. */
    internal fun resetVisitResponseHookSignatures() {
        handledResidualResponseSignatures.clear()
    }

    private fun isTeaTreeSuccess(html: String): Boolean =
        html.contains("You acquire an item", ignoreCase = true)

    private fun extractChoiceDecision(url: String): Int =
        Regex("""(?:^|[?&])(?:option|decision)=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: 0

    private fun shouldSkipTavernExplore(state: CharacterState?): Boolean {
        if (state == null) return false
        return state.adventuresLeft == 0 ||
            state.currentHp == 0 ||
            state.inebriety > state.inebrietyLimit
    }

    private fun extractDescItemId(url: String): String? =
        Regex("""whichitem=(\d+)""").find(url)?.groupValues?.getOrNull(1)

    private fun extractUseQuantity(url: String): Int =
        Regex("""quantity=(\d+)""").find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1

    private fun extractDescEffectId(url: String): String? =
        Regex("""whicheffect=([0-9a-zA-Z]+)""").find(url)?.groupValues?.getOrNull(1)

    private fun extractDescSkillId(url: String): Int? =
        Regex("""whichskill=(\d+)""").find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()

    internal fun processVisitQuestHooks(html: String, url: String? = null) {
        processVisitResponseHooks(html, url)
        if (url?.contains("choice.php", ignoreCase = true) == true) {
            val choice = Regex("""whichchoice=(\d+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.get(1).orEmpty()
            SpadingManager.processChoiceVisit(choice, html, preferences, sessionLogger)
        } else if (url?.contains("fight.php", ignoreCase = true) == true) {
            SpadingManager.processCombatRound(
                preferences?.getString(Preferences.LAST_MONSTER, "").orEmpty(),
                html,
                preferences,
                sessionLogger,
            )
        }
        val prefs = preferences
        if (url != null && prefs != null) {
            syncBastilleVisitFromUrl(html, url, prefs)
            TelescopeSync.parseResponse(
                url,
                html,
                prefs,
                character,
            )
        }
        val db = questDatabase ?: return
        if (TowerSync.containsTowerMarker(html) ||
            url?.contains("tower.php", ignoreCase = true) == true ||
            url?.contains("nstower", ignoreCase = true) == true
        ) {
            SorceressLairSync.parseTowerResponse(
                action = url?.let(SorceressLairSync::action),
                html = html,
                questDatabase = db,
                preferences = prefs,
                setKingLiberated = { character?.setKingLiberated(true) },
            )
        }
        if (url?.contains(TowerDoorConfig.DOOR_PLACE, ignoreCase = true) == true ||
            url?.contains(TowerDoorConfig.LOW_KEY_DOOR_PLACE, ignoreCase = true) == true
        ) {
            TowerSync.parseTowerDoorResponse(
                TowerDoorConfig.extractDoorAction(url),
                html,
                prefs,
                db,
                character?.state?.value,
            )
        }
        if (url?.contains("realm_pirate", ignoreCase = true) == true ||
            url?.let { extractQuestPlace(it) } == "realm_pirate"
        ) {
            PirateRealmSync.parseResponse(html, db, prefs)
        }
        kotlinx.coroutines.runBlocking {
            QuestLogSync.processResponse(html, db, questLogRequest, buildQuestSyncContext(url))
        }
    }

    internal fun buildQuestSyncContext(urlOrPath: String? = null): QuestLogSync.QuestSyncContext =
        QuestLogSync.QuestSyncContext(
            hasItemId = { id -> inventoryManager?.state?.value?.items?.containsKey(id) == true },
            place = urlOrPath?.let { extractQuestPlace(it) },
            url = urlOrPath,
            preferences = preferences,
            currentRun = character?.state?.value?.currentRun ?: 0,
            gameDatabase = gameDatabase,
            consumeItem = { itemId, quantity ->
                inventoryManager?.consumeItemLocally(itemId, quantity)
            },
        )

    internal fun buildCurrentModifiers(): CurrentModifiers {
        val state = character?.state?.value ?: CharacterState()
        val effects = effectManager?.state?.value?.effects ?: emptyList()
        return CurrentModifiers(state, effects, resolvedSkillNames())
    }

    internal fun resolvedSkillNames(): Set<String> {
        val apiSkills = skillManager?.state?.value?.skills
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
        val db = gameDatabase ?: return apiSkills
        val equipmentSkills = SkillGrantingEquipmentSync.grantedSkillNames(buildCheckContext(), db)
        return apiSkills + equipmentSkills
    }

    /**
     * Context for evaluating monster Init/Atk: [expr]
     * (pref + KW/KV/KC + MUS/MOX/ML/… + smithsness K).
     */
    internal fun buildMonsterExpressionContext(): ExpressionContext {
        val prefs = preferences
        val kisses = dreadKissesTracker
        val state = character?.state?.value ?: CharacterState()
        val mods = buildCurrentModifiers()
        val ml = CombatAdjustment.monsterLevelAdjustment(
            mods,
            state,
            lastLocationName(),
        )
        return ExpressionContext(
            level = state.level,
            ascensions = state.ascensionNumber,
            audience = state.audience,
            challengePath = state.challengePath,
            className = state.className,
            smithsness = mods.values.get(DoubleModifier.SMITHSNESS),
            prefLookup = { name -> prefs?.getString(name, "") ?: "" },
            dreadKissWoods = kisses?.kissesForLocation("Woods")?.toInt() ?: 1,
            dreadKissVillage = kisses?.kissesForLocation("Village")?.toInt() ?: 1,
            dreadKissCastle = kisses?.kissesForLocation("Castle")?.toInt() ?: 1,
            buffedMuscle = state.buffedMusc,
            buffedMysticality = state.buffedMyst,
            buffedMoxie = state.buffedMoxie,
            monsterLevel = ml,
            mindControlLevel = state.mindControlLevel,
            basementLevel = preferences?.getInt("basementLevel", 0)?.takeIf { it > 0 }
                ?: net.sourceforge.kolmafia.request.BasementSync.basementLevel,
            characterMaxHp = state.maxHp,
            equippedItemNames = state.equippedItems()
                .map { it.second.lowercase() }
                .filter { it.isNotBlank() }
                .toSet(),
            inBeecore = state.inBeecore,
        )
    }

    /** Context for evaluating restore bracket expressions on `$item[minhp|maxhp|minmp|maxmp]`. */
    internal fun buildRestoreExpressionContext(): ExpressionContext {
        val state = character?.state?.value ?: CharacterState()
        val effects = effectManager?.state?.value?.effects ?: emptyList()
        val skillNames = skillManager?.state?.value?.skills
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
        return ExpressionContext(
            level = state.level,
            inebriety = state.inebriety,
            fullness = state.fullness,
            spleenUsed = state.spleenUsed,
            familiarWeight = state.familiarWeight,
            ascensions = state.ascensionNumber,
            effectsCount = effects.size,
            fury = state.fury,
            audience = state.audience,
            gender = state.gender.modifierValue,
            telescopeUpgrades = state.telescopeUpgrades,
            activeEffects = effects.associate { it.name.lowercase() to it.duration },
            skills = skillNames.map { it.lowercase() }.toSet(),
            challengePath = state.challengePath,
            className = state.className,
            isRestricted = state.isRestricted,
            familiarName = state.familiarName.lowercase(),
            mainhandItemName = state.equipment[EquipmentSlot.WEAPON]?.lowercase() ?: "",
            prefLookup = { name -> preferences?.getString(name, "") ?: "" },
            characterMaxHp = state.maxHp,
            characterMaxMp = state.maxMp,
            characterCurrentHp = state.currentHp,
            equippedItemNames = state.equippedItems()
                .map { it.second.lowercase() }
                .filter { it.isNotBlank() }
                .toSet(),
            inBeecore = state.inBeecore,
        )
    }

    /** Context for `$item[dailyusesleft]` including fight/choice/limit-mode/path guards. */
    internal fun buildItemUseLimitsContext(): ItemUseLimitsContext {
        val state = character?.state?.value ?: CharacterState()
        val prefs = preferences
        return ItemUseLimitsContext(
            character = state,
            preferences = prefs,
            expressionContext = buildRestoreExpressionContext(),
            inMultiFight = adventureManager?.inMultiFight == true,
            choiceFollowsFight = adventureManager?.fightFollowsChoice == true,
            inChoiceAdventure = adventureManager?.inChoiceResolution == true,
            canWalkAwayFromChoice = adventureManager?.canWalkAwayFromChoice() ?: true,
            canUsePotions = !state.inRobocore || YouRobotManager.canUsePotions(),
            accessibleCount = { itemId ->
                inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
            },
        )
    }

    /** Desktop garbage-shirt XP ×2 when shirt equipped and charge remains. */
    internal fun garbageShirtXpMultiplier(): Int {
        val state = character?.state?.value ?: return 1
        val shirt = state.equippedItem(net.sourceforge.kolmafia.character.EquipmentSlot.SHIRT)
            ?.lowercase()
            .orEmpty()
        if (shirt != "makeshift garbage shirt") return 1
        val prefs = preferences ?: return 1
        val charge = prefs.getString("garbageShirtCharge", "")
            .toIntOrNull()
            ?: prefs.getInt("garbageShirtCharge", 0)
        return if (charge > 0) 2 else 1
    }

    internal fun extractQuestPlace(urlOrPath: String): String? =
        extractGuildPlace(urlOrPath)
            ?: when {
                urlOrPath.contains("whichplace=nstower", ignoreCase = true) -> "nstower"
                urlOrPath.contains("tower.php", ignoreCase = true) -> "fern"
                urlOrPath.contains("fernruin", ignoreCase = true) -> "fernruin"
                urlOrPath.contains("whichplace=cemetery", ignoreCase = true) -> "cemetery"
                urlOrPath.contains("whichplace=realm_pirate", ignoreCase = true) -> "realm_pirate"
                else -> null
            }

    internal fun extractGuildPlace(urlOrPath: String): String? =
        Regex("(?:^|[?&])place=([a-z]+)", RegexOption.IGNORE_CASE)
            .find(urlOrPath)
            ?.groupValues
            ?.get(1)
            ?.lowercase()

    internal fun cliSend(parameters: String, isMeat: Boolean, rt: AshRuntimeContext) {
        val normalized = parameters.replace(Regex("(?i)(?:^| )to "), " => ")
        val parts = normalized.split(" => ", limit = 2)
        if (parts.size != 2) {
            rt.print("Invalid send request.")
            return
        }
        var recipientPart = parts[1].trim()
        var message = net.sourceforge.kolmafia.request.SendMailRequest.DEFAULT_MESSAGE
        val sep = recipientPart.indexOf("||")
        if (sep >= 0) {
            message = recipientPart.substring(sep + 2).trim()
            recipientPart = recipientPart.substring(0, sep).trim()
        }
        val recipient = recipientPart
        val itemPart = parts[0].trim().trimEnd(',')
        if (itemPart.isBlank()) {
            kotlinx.coroutines.runBlocking { sendMailRequest?.send(recipient, message) }
            return
        }
        var meat = 0L
        val attachments = mutableListOf<net.sourceforge.kolmafia.request.MailAttachment>()
        val itemSpecs = if (itemPart.contains(',')) {
            itemPart.split(',').map { it.trim() }.filter { it.isNotBlank() }
        } else {
            listOf(itemPart)
        }
        for (spec in itemSpecs) {
            val match = Regex("^(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE).find(spec) ?: continue
            val qty = match.groupValues[1].toLongOrNull() ?: continue
            val name = match.groupValues[2].trim()
            if (name.equals("meat", ignoreCase = true)) {
                if (!isMeat) {
                    rt.print("Please use 'csend' if you need to transfer meat.")
                    return
                }
                meat += qty
                continue
            }
            if (isMeat) continue
            val itemId = gameDatabase?.item(name)?.id
            if (itemId == null) {
                rt.print("Unknown item: $name")
                return
            }
            val available = inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
            if (available < qty) {
                rt.print("[$qty $name] requested, but only $available available.")
                return
            }
            attachments.add(net.sourceforge.kolmafia.request.MailAttachment(itemId, qty.toInt()))
        }
        if (attachments.size > net.sourceforge.kolmafia.request.SendMailRequest.MAX_ATTACHMENTS) {
            rt.print("Too many attachments.")
            return
        }
        if (isMeat && meat > 0) {
            val activeEffectNames = effectManager?.state?.value?.effects
                ?.map { it.name.lowercase() }
                ?.toSet()
                ?: emptySet()
            val db = buffBotDatabase ?: BuffBotDatabase.instance
            val offering = db.getOffering(recipient, meat, activeEffectNames, gameDatabase)
            offering.abortMessage?.let { message ->
                rt.print(message)
                return
            }
            offering.conversionMessage?.let { rt.print(it) }
            meat = offering.meatAmount
        }
        kotlinx.coroutines.runBlocking {
            val mailResult = sendMailRequest?.send(recipient, message, attachments, meat)
            if (mailResult?.isFailure == true && attachments.isNotEmpty() && meat == 0L) {
                sendGiftRequest?.send(recipient, message, attachments)
                    ?: mailResult
            } else {
                mailResult
            }
        }
    }

    internal fun cliGift(parameters: String, rt: AshRuntimeContext) {
        val normalized = parameters.replace(Regex("(?i)(?:^| )to "), " => ")
        val parts = normalized.split(" => ", limit = 2)
        if (parts.size != 2) {
            rt.print("Invalid gift request.")
            return
        }
        var recipientPart = parts[1].trim()
        var message = SendMailRequest.DEFAULT_MESSAGE
        val sep = recipientPart.indexOf("||")
        if (sep >= 0) {
            message = recipientPart.substring(sep + 2).trim()
            recipientPart = recipientPart.substring(0, sep).trim()
        }
        val recipient = recipientPart
        val itemPart = parts[0].trim().trimEnd(',')
        if (itemPart.isBlank()) {
            rt.print("Invalid gift request.")
            return
        }
        val attachments = parseSendAttachments(itemPart, rt) ?: return
        if (attachments.isEmpty()) {
            rt.print("Invalid gift request.")
            return
        }
        kotlinx.coroutines.runBlocking {
            sendGiftRequest?.send(recipient, message, attachments)
        }
    }

    private fun parseSendAttachments(
        itemPart: String,
        rt: AshRuntimeContext,
    ): List<net.sourceforge.kolmafia.request.MailAttachment>? {
        val attachments = mutableListOf<net.sourceforge.kolmafia.request.MailAttachment>()
        val itemSpecs = if (itemPart.contains(',')) {
            itemPart.split(',').map { it.trim() }.filter { it.isNotBlank() }
        } else {
            listOf(itemPart)
        }
        for (spec in itemSpecs) {
            val match = Regex("^(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE).find(spec) ?: continue
            val qty = match.groupValues[1].toLongOrNull() ?: continue
            val name = match.groupValues[2].trim()
            if (name.equals("meat", ignoreCase = true)) {
                rt.print("Please use 'csend' if you need to transfer meat.")
                return null
            }
            val itemId = gameDatabase?.item(name)?.id
            if (itemId == null) {
                rt.print("Unknown item: $name")
                return null
            }
            val available = inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
            if (available < qty) {
                rt.print("[$qty $name] requested, but only $available available.")
                return null
            }
            attachments.add(net.sourceforge.kolmafia.request.MailAttachment(itemId, qty.toInt()))
        }
        if (attachments.size > SendMailRequest.MAX_ATTACHMENTS) {
            rt.print("Too many attachments.")
            return null
        }
        return attachments
    }

    internal fun cliChoice(
        choiceId: Int,
        option: Int,
        extraFormFields: Map<String, String> = emptyMap(),
    ) {
        val req = choiceRequest ?: return
        val db = questDatabase ?: return
        val prefs = preferences
        val bastilleContext = bastilleSyncContext()
        if (prefs != null && BastilleBattalionSync.isBastilleChoice(choiceId)) {
            BastilleBattalionSync.registerRequest(choiceId, option, prefs, bastilleContext)
            BastilleBattalionSync.syncPreChoice(choiceId, option, prefs, bastilleContext)
        }
        kotlinx.coroutines.runBlocking {
            req.choose(choiceId, option, extraFormFields).onSuccess { (html, _) ->
                QuestLogSync.processResponse(html, db, questLogRequest, buildQuestSyncContext())
                if (prefs != null && BastilleBattalionSync.isBastilleChoice(choiceId)) {
                    val effectNames = effectManager?.state?.value?.effects?.map { it.name }?.toSet()
                        ?: emptySet()
                    BastilleBattalionSync.syncPostChoice(
                        choiceId, option, html, prefs, effectNames, bastilleContext,
                    )
                }
                if (prefs != null && choiceId == BarrelChoiceMapper.CHOICE_ID) {
                    BarrelShrineSync.syncPostChoice(option, prefs)
                }
                QuestChoiceRules.apply(choiceId, html, db, option, preferences, inventoryManager,
                    ascensionNumber = character?.state?.value?.ascensionNumber ?: 0,
                    dayCount = character?.state?.value?.dayCount ?: 0,
                    hasCandyCaneSwordEquipped = character?.state?.value?.equipment?.values
                        ?.any { it.contains("candy cane sword", ignoreCase = true) } == true,
                    inPokefam = character?.state?.value?.inPokefam == true,
                    hasItemEquipped = { itemId ->
                        val name = ItemDatabase.getById(itemId)?.name ?: return@apply false
                        character?.state?.value?.equipment?.values
                            ?.any { it.equals(name, ignoreCase = true) } == true
                    },
                    turnsPlayed = character?.state?.value?.turnsPlayed ?: 0,
                    currentRun = character?.state?.value?.currentRun ?: 0,
                    resyncQuestLogPage1 = {
                        kotlinx.coroutines.runBlocking { questLogRequest?.syncPage(1) }
                    },
                    setLimitMode = { mode -> character?.updateLimitMode(mode) },
                    character = character,
                    skillManager = skillManager,
                    choiceUrl = extraFormFields.entries.joinToString("&") { "${it.key}=${it.value}" },
                    adjustFullness = { delta ->
                        val s = character?.state?.value ?: return@apply
                        character.updateConsumables(
                            fullness = (s.fullness + delta).coerceAtLeast(0),
                            inebriety = s.inebriety,
                            spleenUsed = s.spleenUsed,
                        )
                    },
                    adjustSpleen = { delta ->
                        val s = character?.state?.value ?: return@apply
                        character.updateConsumables(
                            fullness = s.fullness,
                            inebriety = s.inebriety,
                            spleenUsed = (s.spleenUsed + delta).coerceAtLeast(0),
                        )
                    },
                    familiarRace = familiarManager?.state?.value?.activeFamiliar?.race.orEmpty(),
                    familiarHasAttribute = { attr ->
                        val id = familiarManager?.state?.value?.activeFamiliar?.id ?: return@apply false
                        FamiliarDefinitionDatabase.getById(id)?.attributes?.contains(attr) == true
                    },
                    lastVisitedLocationName = preferences?.getString(Preferences.LAST_LOCATION, "").orEmpty(),
                    setKingLiberated = { character?.setKingLiberated(true) },
                    sessionLog = { line -> sessionLogger?.appendRawLine(line) },
                    checkDartPerks = {
                        val db = gameDatabase ?: return@apply
                        val visits = DynamicItemModifierSync.checkOwnedItemDescriptions(
                            buildCheckContext(),
                            db,
                            listOf(DartPerksChoiceSync.HOLSTER_NAME),
                        )
                        for (visit in visits) {
                            visitKolPage(visit.path)
                        }
                    },
                    banishManager = banishManager,
                    currentFamiliarId = { familiarManager?.state?.value?.activeFamiliar?.id },
                    clearActiveFamiliar = { familiarManager?.clearActiveFamiliarLocally() },
                    refreshStatus = {
                        val char = character ?: return@apply
                        kotlinx.coroutines.runBlocking {
                            CharacterStatusRefresh.refreshWithQuantumPreflight(
                                characterRequest = characterRequest,
                                character = char,
                                effectManager = effectManager,
                                preferences = preferences,
                                familiarManager = familiarManager,
                                equipmentManager = equipmentManager,
                            )
                        }
                    },
                    hasBoxingDayBreakfast = effectManager?.state?.value?.effects
                        ?.any { it.name.equals("Boxing Day Breakfast", ignoreCase = true) } == true,
                    setMindControlLevel = { level -> character?.setMindControlLevel(level) },
                    hasSkill = { id ->
                        skillManager?.state?.value?.skills?.any { it.id == id } == true
                    },
                    learnSkill = { id ->
                        preferences?.let { prefs ->
                            SkillLearner.learnSkill(id, prefs, skillManager)
                        } ?: skillManager?.learnLocalSkill(
                            SkillData(
                                id = id,
                                name = "skill$id",
                                type = SkillType.PASSIVE,
                                mpCost = 0,
                                dailyLimit = 0,
                                timesCast = 0,
                            ),
                        )
                    },
                    forgetSkill = { id -> skillManager?.forgetLocalSkill(id) },
                    forgetSkillByName = { name ->
                        skillManager?.state?.value?.skills
                            ?.firstOrNull { it.name.equals(name, ignoreCase = true) }
                            ?.let { skillManager?.forgetLocalSkill(it.id) }
                    },
                    resetAfterAvatar = { className ->
                        sessionLogger?.appendRawLine("Now walking on the $className road.")
                    },
                    currentMonsterName = MonsterStatusTracker.getLastMonsterName(),
                )
            }
        }
    }

    private fun bastilleSyncContext(): BastilleSyncContext =
        BastilleSyncContext(
            sessionLogger = sessionLogger,
            playerId = character?.state?.value?.playerId ?: 0,
        )

    private fun syncBastilleVisitFromUrl(html: String, url: String, prefs: Preferences) {
        val bastilleContext = bastilleSyncContext()
        if (url.contains("forceoption=0")) {
            BastilleBattalionSync.syncVisit(
                BastilleBattalionSync.CHOICE_RIG, html, url, prefs, bastilleContext,
            )
        }
        val choiceId = WHICH_CHOICE_URL_PATTERN.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: net.sourceforge.kolmafia.adventure.choice.ChoiceUtilities.extractChoiceId(html)
        if (choiceId != null && BastilleBattalionSync.isBastilleChoice(choiceId)) {
            BastilleBattalionSync.syncVisit(choiceId, html, url, prefs, bastilleContext)
        }
    }

    private val WHICH_CHOICE_URL_PATTERN = Regex("""whichchoice=(\d+)""", RegexOption.IGNORE_CASE)

    internal fun cliGuzzlrAbandon(rt: AshRuntimeContext) {
        val prefs = preferences ?: return
        val db = questDatabase ?: return
        if (db.getProgress(Quest.GUZZLR) == QuestDatabase.UNSTARTED) {
            rt.print("You don't have a client.")
            return
        }
        if (prefs.getBoolean("_guzzlrQuestAbandoned", false)) {
            rt.print("You already abandoned a client today.")
            return
        }
        visitKolPage("inventory.php?tap=guzzlr")
        cliChoice(1412, 1)
        cliChoice(1412, 5)
    }

    internal fun cliGuzzlrAccept(tier: String, rt: AshRuntimeContext) {
        val prefs = preferences ?: return
        val db = questDatabase ?: return
        if (db.getProgress(Quest.GUZZLR) != QuestDatabase.UNSTARTED) {
            rt.print("You already have a client, and need to abandon that client first.")
            return
        }
        val option = when (tier) {
            "bronze" -> 2
            "gold" -> {
                if (prefs.getInt("guzzlrBronzeDeliveries", 0) < 5) {
                    rt.print("You need to make 5 bronze deliveries to serve gold clients.")
                    return
                }
                3
            }
            "platinum" -> {
                if (prefs.getInt("guzzlrGoldDeliveries", 0) < 5) {
                    rt.print("You need to make 5 gold deliveries to serve platinum clients.")
                    return
                }
                4
            }
            else -> {
                rt.print("Use command 'guzzlr accept [bronze | gold | platinum]'")
                return
            }
        }
        visitKolPage("inventory.php?tap=guzzlr")
        cliChoice(1412, option)
    }

    internal fun cliMaze(tag: String, rt: AshRuntimeContext) {
        val db = questDatabase ?: return
        visitKolPage("place.php?whichplace=nstower", applyQuestHooks = true)
        val mode = hedgeMazeModeFromTag(tag)
        if (mode == null) {
            rt.print("What do you mean by '$tag'?")
            return
        }
        val status = db.getProgress(Quest.FINAL)
        if (status != "step4") {
            rt.print(hedgeMazeErrorMessage(status, db))
            return
        }
        if (!applyHedgeMazeMode(mode)) {
            rt.print("Could not configure hedge maze.")
            return
        }
        runHedgeMaze(mode) { message -> rt.print(message) }
    }

    internal fun cliTowerDoorStatus(rt: AshRuntimeContext, neededOnly: Boolean) {
        val charState = character?.state?.value ?: return
        val prefs = preferences ?: return
        rt.print(TowerDoorStatus.buildTable(charState, prefs, inventoryManager, gameDatabase, neededOnly))
    }

    internal fun cliDoor(rt: AshRuntimeContext) {
        val db = questDatabase ?: return
        visitKolPage("place.php?whichplace=nstower", applyQuestHooks = true)
        val status = db.getProgress(Quest.FINAL)
        if (status != "step5") {
            rt.print(TowerDoorConfig.towerDoorErrorMessage(status, db))
            return
        }
        runTowerDoor { message -> rt.print(message) }
    }

    internal fun dispatchModeableCli(modeable: Modeable, mode: String, rt: AshRuntimeContext) {
        val request = modeableRequest
        if (request == null) {
            rt.print("Mode command unavailable")
            return
        }
        if (mode.isBlank()) {
            val current = ModeableState.currentMode(preferences, modeable)
            rt.print("Current ${modeable.command}: $current")
            rt.print("Available modes: ${modeable.modes.sorted().joinToString(", ")}")
            rt.print("Usage: ${modeable.command} <mode>")
            return
        }
        kotlinx.coroutines.runBlocking {
            request.setMode(modeable, mode)
                .onSuccess { rt.print("${modeable.command} $mode") }
                .onFailure { rt.print("Mode change failed: ${it.message}") }
        }
    }

    internal fun cliKgb(rest: String, rt: AshRuntimeContext) {
        val request = kgbRequest
        if (request == null) {
            rt.print("KGB request unavailable")
            return
        }
        val trimmed = rest.trim()
        if (trimmed.isEmpty()) {
            val clicks = preferences?.getInt("_kgbClicksUsed", 0) ?: 0
            val dispenser = preferences?.getInt("_kgbDispenserUses", 0) ?: 0
            rt.print("KGB clicks used: $clicks")
            rt.print("KGB dispenser uses: $dispenser")
            return
        }
        val tokens = trimmed.split(Regex("\\s+"))
        val verb = tokens[0]
        when {
            verb.equals("button", ignoreCase = true) && tokens.size < 2 -> {
                rt.print("Usage: kgb button <action>")
                return
            }
            verb.equals("dispenser", ignoreCase = true) && tokens.size < 2 -> {
                rt.print("Usage: kgb dispenser <itemId>")
                return
            }
        }
        kotlinx.coroutines.runBlocking {
            val result = when {
                verb.equals("button", ignoreCase = true) ->
                    request.button(tokens.drop(1).joinToString(" "))
                verb.equals("dispenser", ignoreCase = true) -> {
                    val itemId = tokens.getOrNull(1)?.toIntOrNull()
                    if (itemId == null) {
                        Result.failure(IllegalArgumentException("KGB dispenser requires an item id."))
                    } else {
                        request.dispenser(itemId)
                    }
                }
                verb.equals("visit", ignoreCase = true) -> request.visit()
                else -> request.button(verb)
            }
            result
                .onSuccess { rt.print("kgb $trimmed") }
                .onFailure { rt.print(it.message ?: "KGB request failed") }
        }
    }

    internal fun cliEquip(rest: String, rt: AshRuntimeContext? = null) {
        val equipment = character?.state?.value?.equipment ?: emptyMap()
        val spaceIdx = rest.indexOf(' ')
        if (spaceIdx > 0) {
            val firstToken = rest.substring(0, spaceIdx)
            val afterFirst = rest.substring(spaceIdx + 1).trim()
            val knownSlot = EquipmentSlot.entries.find { s ->
                s.apiKey.equals(firstToken, ignoreCase = true)
            } ?: when {
                firstToken.equals("familiar", ignoreCase = true) -> EquipmentSlot.FAMILIAR
                firstToken.equals("off-hand", ignoreCase = true) -> EquipmentSlot.OFFHAND
                else -> null
            }
            if (knownSlot != null) {
                val item = inventoryManager?.state?.value?.items?.values
                    ?.find { it.name.equals(afterFirst, ignoreCase = true) }
                if (item != null) {
                    val current = equipment[knownSlot]
                    if (current != null && current.equals(item.name, ignoreCase = true)) {
                        rt?.print("Item ${item.name} is already equipped.")
                        return
                    }
                    if (knownSlot in EquipmentSlot.CODPIECE_SLOTS) {
                        kotlinx.coroutines.runBlocking {
                            equipmentRequest?.equipItem(item.itemId, knownSlot)
                        }
                    } else {
                        kotlinx.coroutines.runBlocking {
                            inventoryManager?.equipItem(item, knownSlot.apiKey)
                        }
                    }
                    return
                }
            }
        }
        val item = inventoryManager?.state?.value?.items?.values
            ?.find { it.name.equals(rest, ignoreCase = true) }
        if (item != null) {
            val alreadyEquipped = equipment.values.any { it.equals(item.name, ignoreCase = true) }
            if (alreadyEquipped) {
                rt?.print("Item ${item.name} is already equipped.")
                return
            }
            kotlinx.coroutines.runBlocking { inventoryManager?.equipItem(item, "default") }
        }
    }

    fun updateOneDesc() {
        DescriptionConsequenceSync.pathForToday()?.let { visitKolPage(it) }
    }

    fun checkDynamicModifiers() {
        val prefs = preferences ?: return
        val db = gameDatabase ?: return
        if (!prefs.getBoolean("_pulverizationInitialized", false)) {
            EquipmentDatabase.initializePulverization()
            prefs.setBoolean("_pulverizationInitialized", true)
        }
        val context = buildCheckContext()
        val currentClass = character?.state?.value?.className.orEmpty()
        val previousClass = prefs.getString("_lastKnownClass", "")
        val playerClassChanged = previousClass.isNotBlank() && previousClass != currentClass
        val visits = (
            DynamicItemModifierSync.checkMods(prefs, context, db, playerClassChanged) +
                DynamicItemModifierSync.checkExtendedMods(prefs, context, db) +
                DynamicItemModifierSync.checkLoginDescChecks(prefs, context, db) +
                DynamicItemModifierSync.checkOwnedItemDescriptions(
                    context,
                    db,
                    DynamicItemModifierSync.OWNED_DESC_ITEMS,
                )
            ).distinctBy { it.path }
        for (visit in visits) {
            visitKolPage(visit.path)
        }
        val state = character?.state?.value
        SeptEmberSync.checkBalance(
            prefs = prefs,
            accessibleCount = { itemId ->
                kotlinx.coroutines.runBlocking {
                    physicalAccessibleCount(
                        itemId,
                        db.item(itemId)?.name ?: return@runBlocking 0,
                    )
                }
            },
            isKingdomOfExploathing = state?.isKingdomOfExploathing == true,
            onVisit = { visitKolPage(SeptEmberSync.SHOP_PATH) },
        )
        if (currentClass.isNotBlank()) {
            prefs.setString("_lastKnownClass", currentClass)
        }
    }

    internal suspend fun runPulverizeCli(itemNames: List<String>, explicitQty: Int?) {
        val request = pulverizeRequest ?: return
        val db = gameDatabase ?: return
        for (itemName in itemNames) {
            val itemId = db.item(itemName)?.id ?: continue
            val qty = explicitQty ?: inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
            if (qty <= 0) continue
            request.pulverize(itemId, qty)
        }
    }

    internal suspend fun runUntinkerQuestCli() {
        untinkerRequest?.completeQuest()
    }

    internal suspend fun runUntinkerCli(itemNames: List<String>) {
        val request = untinkerRequest ?: return
        val db = gameDatabase ?: return
        val llScrewdriverName = db.item(UntinkerRequest.LOATHING_LEGION_SCREWDRIVER)?.name
            ?: "Loathing Legion universal screwdriver"
        val useLegion = physicalAccessibleCount(
            UntinkerRequest.LOATHING_LEGION_SCREWDRIVER,
            llScrewdriverName,
        ) > 0
        for (itemName in itemNames) {
            val itemId = db.item(itemName)?.id ?: continue
            val qty = inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
            if (qty <= 0) continue
            if (useLegion) {
                request.untinkerViaLegionScrewdriver(itemId, qty)
            } else {
                request.untinker(itemId, qty)
            }
        }
    }

    internal suspend fun runCleanupJunkCli() {
        cleanupJunkRunner?.cleanup()
    }

    internal suspend fun runAutoMallCli() {
        autoMallRunner?.automall()
    }

    internal suspend fun runQuarkCli(itemNames: List<String>) {
        quarkRunner?.quark(itemNames) { message ->
            lastCliOutput.appendLine(message)
        }
    }

    internal suspend fun runZapCli(itemNames: List<String>) {
        val request = zapRequest ?: return
        val db = gameDatabase ?: return
        for (itemName in itemNames) {
            val itemId = db.item(itemName)?.id ?: continue
            val qty = inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
            if (qty <= 0) continue
            repeat(qty) {
                request.zap(itemId)
            }
        }
    }

    internal suspend fun physicalAccessibleCount(itemId: Int, itemName: String): Int =
        AccessibleItemCount.physicalCount(
            itemId = itemId,
            itemName = itemName,
            inventoryManager = inventoryManager,
            closetRequest = closetRequest,
            storageRequest = storageRequest,
            displayCaseRequest = displayCaseRequest,
            clanStashRequest = clanStashRequest,
            equipment = character?.state?.value?.equipment ?: emptyMap(),
            context = AccessCountContext(
                characterState = character?.state?.value,
                gameDatabase = gameDatabase,
                familiarManager = familiarManager,
                preferences = preferences,
            ),
        )

    internal suspend fun ensureRestrictionListsInitialized(state: CharacterState?) {
        net.sourceforge.kolmafia.request.RestrictionListRefresh.ensureInitialized(
            state = state,
            standardRequest = standardRequest,
            thriftyRequest = thriftyRequest,
            trendyRequest = trendyRequest,
        )
    }

    internal fun buildCheckContext(): DynamicItemModifierSync.CheckContext {
        val inventoryIds = inventoryManager?.state?.value?.items
            ?.filterValues { it.quantity > 0 }
            ?.keys
            ?.toSet()
            ?: emptySet()
        val equippedNames = character?.state?.value?.equipment
            ?.values
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
        val activeEffects = effectManager?.state?.value?.effects
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
        val closetIds = kotlinx.coroutines.runBlocking {
            closetRequest?.fetchContents()?.keys?.toSet() ?: emptySet()
        }
        val storageIds = kotlinx.coroutines.runBlocking {
            val charState = character?.state?.value
            if (charState?.isRestricted == true) {
                standardRequest?.ensureInitialized()
            }
            if (charState?.isThrifty == true) {
                thriftyRequest?.ensureInitialized()
            }
            if (charState?.isTrendy == true) {
                trendyRequest?.ensureInitialized()
            }
            storageRequest?.fetchContents(charState)
                ?.filterValues { it > 0 }
                ?.keys
                ?.toSet()
                ?: emptySet()
        }
        val stashIds = kotlinx.coroutines.runBlocking {
            clanStashRequest?.fetchContents()
                ?.filterValues { it > 0 }
                ?.keys
                ?.toSet()
                ?: emptySet()
        }
        val charState = character?.state?.value
        val ascensionPath = charState?.ascensionPath ?: AscensionPath.NONE
        val codpieceGemNames = charState?.equipment
            ?.filterKeys { it in EquipmentSlot.CODPIECE_SLOTS }
            ?.values
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
        val hermitCloverCount = kotlinx.coroutines.runBlocking {
            hermitRequest?.fetchCloverCount(ascensionPath, preferences) ?: 0
        }
        return DynamicItemModifierSync.CheckContext(
            inventoryItemIds = inventoryIds,
            equippedItemNames = equippedNames,
            activeEffectNames = activeEffects,
            closetItemIds = closetIds,
            storageItemIds = storageIds,
            stashItemIds = stashIds,
            limitMode = charState?.limitMode.orEmpty(),
            canInteract = charState?.let { !it.isHardcore && !it.isInRonin } ?: true,
            hasClan = charState?.hasClan ?: false,
            ascensionPath = ascensionPath,
            codpieceGemNames = codpieceGemNames,
            hermitCloverCount = hermitCloverCount,
        )
    }

    internal fun fetchDescription(path: String) {
        visitKolPage(path)
    }

    internal fun visitKolPage(path: String, applyQuestHooks: Boolean = false): String? {
        val client = httpClient ?: return null
        val db = questDatabase
        var htmlOut: String? = null
        kotlinx.coroutines.runBlocking {
            try {
                val response = client.get("$KOL_BASE_URL/$path")
                if (!response.status.isSuccess()) return@runBlocking
                val html = response.bodyAsText()
                htmlOut = html
                processVisitResponseHooks(html, "$KOL_BASE_URL/$path")
                if (path.equals("charpane.php", ignoreCase = true) ||
                    path.endsWith("/charpane.php", ignoreCase = true)
                ) {
                    edServantManager?.syncFromCharpane(html)
                    vykeaCompanionManager?.syncFromCharpane(html)
                    pastaThrallManager?.syncFromCharpane(html)
                    character?.let { ClassResourceCharpaneSync.apply(it, html) }
            ClanIdSync.apply(html)
                }
                if (path.contains("edbase", ignoreCase = true) && html.contains("whichchoice=1053")) {
                    edServantManager?.syncFromChoice1053(html)
                }
                if (applyQuestHooks && db != null) {
                    processVisitQuestHooks(html, "$KOL_BASE_URL/$path")
                }
            } catch (_: Exception) {
                // best-effort page visit
            }
        }
        return htmlOut
    }

    /** POST form fields to a KoL path (relative), applying visit hooks. */
    internal fun visitKolPost(path: String, postData: String): String? {
        val client = httpClient ?: return null
        var htmlOut: String? = null
        kotlinx.coroutines.runBlocking {
            try {
                val response = client.submitForm(
                    url = "$KOL_BASE_URL/${path.trimStart('/')}",
                    formParameters = io.ktor.http.Parameters.build {
                        postData.split("&").filter { it.isNotBlank() }.forEach { pair ->
                            val eq = pair.indexOf('=')
                            if (eq >= 0) append(pair.substring(0, eq), pair.substring(eq + 1))
                            else append(pair, "")
                        }
                    },
                )
                val html = response.bodyAsText()
                htmlOut = html
                processVisitResponseHooks(html, "$KOL_BASE_URL/$path")
                processVisitQuestHooks(html, "$KOL_BASE_URL/$path")
            } catch (_: Exception) {
                // best-effort
            }
        }
        return htmlOut
    }

    /** Desktop FightRequest macro submit — action=macro + macrotext. */
    internal fun visitKolFightMacro(macroText: String): String? {
        val client = httpClient ?: return null
        var htmlOut: String? = null
        kotlinx.coroutines.runBlocking {
            try {
                val response = client.submitForm(
                    url = "$KOL_BASE_URL/fight.php",
                    formParameters = io.ktor.http.parameters {
                        append("action", "macro")
                        append("macrotext", macroText)
                    },
                )
                val html = response.bodyAsText()
                htmlOut = html
                processVisitResponseHooks(html, "$KOL_BASE_URL/fight.php")
                processVisitQuestHooks(html, "$KOL_BASE_URL/fight.php")
            } catch (_: Exception) {
                // best-effort
            }
        }
        return htmlOut
    }

    internal fun uneffectParameter(parameter: String) {
        val active = effectManager?.state?.value?.effects.orEmpty()
        if (parameter.contains(',') &&
            active.none { it.name.equals(parameter, ignoreCase = true) }
        ) {
            parameter.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach(::uneffectByName)
            return
        }
        uneffectByName(parameter)
    }

    internal fun uneffectByName(name: String) {
        val active = effectManager?.state?.value?.effects.orEmpty()
        val explicitId = Regex("""^\[(\d+)]$""").matchEntire(name)?.groupValues?.get(1)?.toIntOrNull()
        val matches = active.filter {
            (explicitId != null && it.id == explicitId) ||
                it.name.equals(name, ignoreCase = true) ||
                it.name.contains(name, ignoreCase = true)
        }
        val effect = when {
            matches.size == 1 -> matches.single()
            matches.size > 1 -> {
                val shruggable = matches.filter {
                    net.sourceforge.kolmafia.request.UneffectRequest.isShruggable(it.id)
                }
                if (shruggable.size == 1) shruggable.single() else {
                    sessionLogger?.appendRawLine(
                        "Ambiguous effect name: $name (${matches.joinToString { it.name }})",
                    )
                    return
                }
            }
            else -> return
        }
        val prefs = preferences ?: return
        val charState = character?.state?.value
        val inv = inventoryManager?.state?.value
        val hasItemId: (Int) -> Boolean = { id ->
            (inv?.items?.get(id)?.quantity ?: 0) > 0
        }
        val hasSkill = UneffectRemovableMaps.hasSkillResolver(preferences, skillManager)
        val canCastSkill: (String) -> Boolean = { skillName ->
            skillManager?.state?.value?.skills
                ?.any { it.name.equals(skillName, ignoreCase = true) } == true
        }
        val moodAction = moodManager?.getDefaultAction("gain_effect", effect.name).orEmpty()
        val checkContext = buildCheckContext()
        val accessibleCount: (Int) -> Int = { itemId ->
            kotlinx.coroutines.runBlocking {
                physicalAccessibleCount(itemId, net.sourceforge.kolmafia.data.ItemDatabase.getItemName(itemId))
            }
        }
        lateinit var actionCtx: UneffectActionContext
        actionCtx = UneffectActionContext(
            effectId = effect.id,
            effectName = effect.name,
            moodPredefinedAction = moodAction,
            preferences = prefs,
            characterState = charState,
            hasItemId = hasItemId,
            hasSkill = hasSkill,
            canCastSkill = canCastSkill,
            inGLover = charState?.inGLover == true,
            hasGs = net.sourceforge.kolmafia.character.Beeosity::hasGs,
            canRetrieveRemedy = retrieveItemService != null,
            canAcquireUneffectItem = { itemId ->
                UneffectItemAcquisition.canAcquireUneffectItem(
                    itemId = itemId,
                    effectId = effect.id,
                    ctx = actionCtx,
                    checkContext = checkContext,
                    prefs = prefs,
                    db = gameDatabase,
                    charState = charState,
                    accessibleCount = accessibleCount,
                )
            },
        )
        val action = UneffectActionResolver.resolve(actionCtx)
        if (action is UneffectAction.HttpUneffect &&
            UneffectItemAcquisition.shouldBlockNeedsCocoaHttpUneffect(
                effectId = effect.id,
                ctx = actionCtx,
                checkContext = checkContext,
                prefs = prefs,
                db = gameDatabase,
                charState = charState,
                accessibleCount = accessibleCount,
            )
        ) {
            sessionLogger?.appendRawLine(
                "${effect.name} can be removed only with hot Dreadsylvanian cocoa.",
            )
            return
        }
        executeUneffectAction(action, effect.id)
    }

    private fun executeUneffectAction(action: UneffectAction, effectId: Int) {
        when (action) {
            is UneffectAction.CastSkill -> {
                val skill = skillManager?.state?.value?.skills
                    ?.find { it.name.equals(action.skillName, ignoreCase = true) } ?: return
                kotlinx.coroutines.runBlocking { skillManager.cast(skill) }
            }
            is UneffectAction.HotTub -> {
                kotlinx.coroutines.runBlocking { clanLoungeRequest?.useHotTub(preferences) }
            }
            is UneffectAction.UseItem -> {
                val useReq = useItemRequest
                if (useReq != null) {
                    var used = false
                    kotlinx.coroutines.runBlocking {
                        if (action.retrieveFirst) {
                            retrieveItemService?.retrieve(action.itemId, 1)
                        }
                        val qty = inventoryManager?.state?.value?.items?.get(action.itemId)?.quantity ?: 0
                        if (qty > 0) {
                            useReq.use(action.itemId, 1)
                            used = true
                        }
                    }
                    if (used) return
                }
                val req = uneffectRequest ?: return
                kotlinx.coroutines.runBlocking { req.uneffect(effectId) }
            }
            is UneffectAction.HttpUneffect -> {
                val req = uneffectRequest ?: return
                kotlinx.coroutines.runBlocking { req.uneffect(effectId) }
            }
        }
    }

    internal fun uneffectAll() {
        val req = uneffectRequest ?: return
        val effects = effectManager?.state?.value?.effects ?: return
        kotlinx.coroutines.runBlocking {
            for (effect in effects) {
                req.uneffect(effect.id).onFailure { /* best-effort */ }
            }
        }
    }

    internal fun buildShowAllSummary(): String {
        val cs = character?.state?.value ?: return "Not logged in."
        return buildString {
            append("${cs.name} (#${cs.playerId}) — Level ${cs.level} ${cs.className}")
            append("; HP ${cs.currentHp}/${cs.maxHp}")
            append("; MP ${cs.currentMp}/${cs.maxMp}")
            append("; Mus ${cs.buffedMusc} Mys ${cs.buffedMyst} Mox ${cs.buffedMoxie}")
            append("; ${cs.adventuresLeft} adv; ${cs.meat} meat")
            val loc = preferences?.getString(Preferences.LAST_LOCATION, "") ?: ""
            if (loc.isNotBlank()) append("; loc=$loc")
            if (cs.familiarName.isNotBlank()) append("; familiar=${cs.familiarName}")
        }
    }

    internal fun buildDumpSummary(): String {
        val cs = character?.state?.value
        val loc = preferences?.getString(Preferences.LAST_LOCATION, "") ?: ""
        val goals = goalManager?.allGoalsAsStrings()?.joinToString(", ") ?: ""
        return buildString {
            if (cs != null) {
                append("${cs.name} L${cs.level}; ${cs.adventuresLeft} adv; ${cs.meat} meat")
            }
            if (loc.isNotBlank()) append("; loc=$loc")
            if (goals.isNotBlank()) append("; goals=[$goals]")
        }
    }

    internal fun dispatchCli(cmd: String, rt: AshRuntimeContext) {
        var remaining = cmd.trim()
        if (remaining.isNotEmpty()) {
            val firstWord = remaining.split(Regex("\\s+"), limit = 2).first()
            if (!firstWord.equals("repeat", ignoreCase = true)) {
                previousLine = remaining
            }
        }
        while (remaining.isNotEmpty()) {
            val expandedLine = expandCliAlias(remaining)
            val fullCommand = expandedLine.split(Regex("\\s+"), limit = 2).first()
            if (CliConditional.isFullLine(fullCommand)) {
                val params = expandedLine.substring(fullCommand.length).trim()
                val word = fullCommand.lowercase().removeSuffix("?")
                when (word) {
                    "cheapest", "expensive" -> runComparisonShopCli(fullCommand, params, rt)
                    "get", "set" -> runSetGetCli(word, params, rt)
                    "alias" -> runAliasCli(params, rt)
                }
                return
            }
            val splitIndex = remaining.indexOf(';')
            val segment: String
            val restAfter: String
            if (splitIndex != -1) {
                segment = remaining.substring(0, splitIndex).trim()
                restAfter = remaining.substring(splitIndex + 1).trim()
            } else {
                segment = remaining.trim()
                restAfter = ""
            }
            remaining = restAfter
            if (segment.isEmpty()) continue
            val expanded = expandCliAlias(segment)
            val command = expanded.split(Regex("\\s+"), limit = 2).first()
            if (CliConditional.isFlowControl(command)) {
                val continuation = flowContinuation(restAfter)
                if (continuation == null) {
                    rt.print("Unterminated conditional statement.")
                    return
                }
                val params = expanded.substring(command.length).trim()
                runFlowControl(command, params, continuation, rt)
                previousLine = "$command $params;$continuation"
                return
            }
            dispatchCliSegment(expanded, rt)
        }
    }

    private fun runFlowControl(
        command: String,
        parameters: String,
        continuation: String,
        rt: AshRuntimeContext,
    ) {
        when (command.lowercase().removeSuffix("?")) {
            "if" -> runIfCli(parameters, continuation, rt)
            "while" -> runWhileCli(parameters, continuation, rt)
            "else" -> runElseCli(parameters, continuation, rt)
            "elseif" -> runElseIfCli(parameters, continuation, rt)
            "try" -> runTryCli(parameters, continuation, rt)
        }
    }

    private fun dispatchCliSegment(cmd: String, rt: AshRuntimeContext) {
        val expanded = expandCliAlias(cmd.trim())
        val matched = cliDispatch.firstOrNull { (regex, _) -> regex.matches(expanded) }
        if (matched != null) {
            matched.second(matched.first.find(expanded)!!, rt)
        } else if (looksLikeVisitUrl(expanded)) {
            runVisitUrlCli(expanded, printHtml = false, rt)
        } else {
            rt.print("[cli] $expanded")
        }
    }

    internal fun listCliAliases(): Map<String, String> {
        val prefs = preferences ?: return emptyMap()
        return prefs.getString(CLI_ALIASES_PREF, "")
            .split('|')
            .mapNotNull { entry ->
                val sep = entry.indexOf("=>")
                if (sep <= 0) return@mapNotNull null
                val name = entry.substring(0, sep).trim()
                val command = entry.substring(sep + 2).trim()
                if (name.isBlank() || command.isBlank()) null else name to command
            }
            .toMap()
    }

    internal fun setCliAlias(name: String, command: String) {
        val prefs = preferences ?: return
        val updated = listCliAliases().toMutableMap()
        updated[name.lowercase()] = command
        prefs.setString(
            CLI_ALIASES_PREF,
            updated.entries.joinToString("|") { "${it.key}=>${it.value}" },
        )
    }

    internal fun removeCliAlias(name: String): Boolean {
        val prefs = preferences ?: return false
        val updated = listCliAliases().toMutableMap()
        if (updated.remove(name.lowercase()) == null) return false
        prefs.setString(
            CLI_ALIASES_PREF,
            updated.entries.joinToString("|") { "${it.key}=>${it.value}" },
        )
        return true
    }

    internal fun expandCliAlias(cmd: String): String {
        val firstSpace = cmd.indexOf(' ')
        val name = if (firstSpace < 0) cmd else cmd.substring(0, firstSpace)
        val rest = if (firstSpace < 0) "" else cmd.substring(firstSpace + 1)
        val alias = listCliAliases()[name.lowercase()] ?: return cmd
        return if (rest.isBlank()) alias else "$alias $rest"
    }

    /** Bridges the protected [register] so extension functions in this module can call it. */
    internal fun regFn(
        scope: AshScope,
        name: String,
        returnType: AshType,
        params: List<Pair<String, AshType>>,
        impl: (AshRuntimeContext, List<AshValue>) -> AshValue
    ) = register(scope, name, returnType, params, impl)

    override fun resolveEntityIndex(base: AshValue, index: AshValue): AshValue? {
        val field = index.toString()
        if (field.isBlank()) return null
        return when (base.type) {
            AshType.SERVANT -> ServantEntityFields.resolve(base.toString(), field, edServantManager)
            AshType.THRALL -> ThrallEntityFields.resolve(
                base.toString(),
                field,
                preferences,
                gameDatabase,
            )
            AshType.VYKEA -> VykeaEntityFields.resolve(base.toString(), field)
            AshType.MONSTER -> {
                val mods = buildCurrentModifiers()
                val state = character?.state?.value
                val ml = CombatAdjustment.monsterLevelAdjustment(mods, state, lastLocationName())
                val ctx = buildMonsterExpressionContext()
                val xpMult = garbageShirtXpMultiplier()
                val reduce = CombatAdjustment.reduceEnemyDefensePercent(mods)
                val monsterName = base.monsterRefName()
                val override = if (base.monsterUseInstance()) {
                    MonsterStatusTracker.getLastMonster()
                        ?.takeIf { it.name.equals(monsterName, ignoreCase = true) }
                        ?.let { inst -> RandomModifierStats.apply(inst, inst.randomModifiers, ctx) }
                } else {
                    null
                }
                MonsterEntityFields.resolve(
                    monsterName,
                    field,
                    gameDatabase,
                    expressionContext = ctx,
                    ml = ml,
                    xpMultiplier = xpMult,
                    reduceEnemyDefensePercent = reduce,
                    characterClass = state?.characterClassEnum ?: CharacterClass.UNKNOWN,
                    ascensionPath = state?.ascensionPath ?: AscensionPath.NONE,
                    monsterOverride = override,
                )
            }
            AshType.LOCATION -> LocationEntityFields.resolve(
                base.toString(),
                field,
                gameDatabase,
                preferences,
                adventureSpentTracker,
                dreadKissesTracker,
                wildfireCampManager,
                character?.state?.value,
            )
            AshType.PATH -> PathEntityFields.resolve(base.toString(), field, preferences)
            AshType.ITEM -> ItemEntityFields.resolve(
                base.toString(),
                field,
                gameDatabase,
                buildRestoreExpressionContext(),
                character?.state?.value ?: CharacterState(),
                preferences,
                buildItemUseLimitsContext(),
            )
            AshType.SKILL -> SkillEntityFields.resolve(
                base.toString(),
                field,
                gameDatabase,
                skillManager,
                preferences,
            )
            AshType.EFFECT -> EffectEntityFields.resolve(
                base.toString(),
                field,
                gameDatabase,
            )
            AshType.FAMILIAR -> FamiliarEntityFields.resolve(
                base.toString(),
                field,
                gameDatabase,
                familiarManager,
                preferences,
                pokeTeam = character?.state?.value?.pokeTeam.orEmpty(),
            )
            AshType.BOUNTY -> BountyEntityFields.resolve(
                base.toString(),
                field,
                gameDatabase,
            )
            AshType.PHYLUM -> PhylumEntityFields.resolve(base.toString(), field)
            AshType.ELEMENT -> ElementEntityFields.resolve(base.toString(), field)
            AshType.MODIFIER -> ModifierEntityFields.resolve(base.toString(), field)
            AshType.CLASS -> ClassEntityFields.resolve(base.toString(), field)
            AshType.COINMASTER -> {
                val inventory = inventoryManager?.state?.value?.items
                    ?.mapValues { (_, item) -> item.quantity }
                    ?: emptyMap()
                CoinmasterEntityFields.resolve(
                    base.toString(),
                    field,
                    preferences,
                    inventory,
                )
            }
            else -> null
        }
    }

    override fun registerAll(scope: AshScope) {
        super.registerAll(scope) // registers print() and to_string() overloads from stub
        registerTypeConversions(scope)
        registerStringUtils(scope)
        registerMathUtils(scope)
        registerAggregateUtils(scope)
        registerAggregateExtensions(scope)
        registerPrintUtils(scope)
        registerCharacterQueries(scope)
        registerItemQueries(scope)
        registerSkillQueries(scope)
        registerEffectQueries(scope)
        registerGameActions(scope)
        // new extension calls (added as tasks T4–T13 are implemented):
        registerCharacterExtensions(scope)
        registerLocationQueries(scope)
        registerFamiliarQueries(scope)
        registerEquipmentQueries(scope)
        registerModifierQueries(scope)
        registerCollectionQueries(scope)
        registerDateTimeQueries(scope)
        registerGoalQueries(scope)
        registerMoodQueries(scope)
        registerPreferenceAccess(scope)
        registerCombatStubs(scope)
        registerCombatScript(scope)
        registerLongTailStubs(scope)
        registerAshP8Batch(scope)
        registerAshP9Batch(scope)
        registerAshP10Batch(scope)
        registerAshP11Batch(scope)
        registerAshP12Batch(scope)
        registerAshP13Batch(scope)
        registerAshP14Batch(scope)
        registerAshP15Batch(scope)
        registerAshP16Batch(scope)
        registerAshP17Batch(scope)
        registerAshP18Batch(scope)
        registerAshP19Batch(scope)
        registerAshP20Batch(scope)
        registerAshP21Batch(scope)
        registerAshP22Batch(scope)
        registerAshP23Batch(scope)
        registerAshP24Batch(scope)
        registerAshP25Batch(scope)
        registerAshP26Batch(scope)
        registerAshP27Batch(scope)
        registerAshP28Batch(scope)
        registerAshP29Batch(scope)
        registerAshP38Batch(scope)
        registerAshP39Batch(scope)
        registerAshP40Batch(scope)
        registerAshP41Batch(scope)
        registerAshP42Batch(scope)
        registerAshP43Batch(scope)
        registerAshP44Batch(scope)
        registerAshP45Batch(scope)
        registerAshP46Batch(scope)
        registerAshP47Batch(scope)
        registerAshP81Batch(scope)
        registerAshP82Batch(scope)
        registerAshP83Batch(scope)
        registerAshP115Batch(scope)
        registerAshP116Batch(scope)
        registerAshP117Batch(scope)
        registerAshP118Batch(scope)
        registerAshP119Batch(scope)
        registerAshP120Batch(scope)
        registerAshP121Batch(scope)
        registerAshP122Batch(scope)
        registerAshP123Batch(scope)
        registerAshP124Batch(scope)
        registerAshP125Batch(scope)
        registerAshP126Batch(scope)
        registerAshP127Batch(scope)
        registerAshP128Batch(scope)
        registerAshP129Batch(scope)
        registerAshP130Batch(scope)
        registerAshP131Batch(scope)
        registerAshP132Batch(scope)
        registerAshP133Batch(scope)
        registerAshP134Batch(scope)
        registerAshP135Batch(scope)
        registerAshP136Batch(scope)
        registerAshP137Batch(scope)
        registerAshP138Batch(scope)
        registerAshP139Batch(scope)
        registerAshP140Batch(scope)
        registerAshP141Batch(scope)
        registerAshP142Batch(scope)
        registerAshP143Batch(scope)
        registerAshP144Batch(scope)
        registerAshP145Batch(scope)
        registerAshP146Batch(scope)
        registerAshP147Batch(scope)
        registerAshP148Batch(scope)
        registerAshP149Batch(scope)
        registerAshP150Batch(scope)
        registerAshP151Batch(scope)
        registerAshP152Batch(scope)
        registerAshP153Batch(scope)
        registerAshP154Batch(scope)
        registerAshP155Batch(scope)
        registerAshP156Batch(scope)
        registerAshP157Batch(scope)
        registerAshP158Batch(scope)
        registerAshP159Batch(scope)
        registerAshP160Batch(scope)
        registerAshP161Batch(scope)
        registerAshP162Batch(scope)
        registerAshP163Batch(scope)
        registerAshP164Batch(scope)
        registerAshP165Batch(scope)
        registerAshP166Batch(scope)
        registerAshP167Batch(scope)
        registerAshP168Batch(scope)
        registerAshP169Batch(scope)
        registerAshP170Batch(scope)
        registerAshP171Batch(scope)
        registerAshP172Batch(scope)
        registerAshP173Batch(scope)
        registerAshP174Batch(scope)
        registerAshP175Batch(scope)
        registerAshP176Batch(scope)
        registerAshP177Batch(scope)
        registerAshP178Batch(scope)
        registerAshP179Batch(scope)
        registerAshP180Batch(scope)
        registerAshP181Batch(scope)
        registerAshP182Batch(scope)
        registerAshP183Batch(scope)
        registerAshP184Batch(scope)
        registerAshP185Batch(scope)
        registerAshP186Batch(scope)
        registerAshP187Batch(scope)
        registerAshP188Batch(scope)
        registerAshP189Batch(scope)
        registerAshP190Batch(scope)
        registerAshP191Batch(scope)
        registerAshP192Batch(scope)
        registerAshP193Batch(scope)
        registerAshP194Batch(scope)
        registerAshP195Batch(scope)
        registerAshP196Batch(scope)
        registerAshP197Batch(scope)
        registerAshP198Batch(scope)
        registerAshP199Batch(scope)
        registerAshP200Batch(scope)
        registerAshP201Batch(scope)
        registerAshP202Batch(scope)
        registerAshP203Batch(scope)
        registerAshP204Batch(scope)
        registerAshP205Batch(scope)
        registerAshP206Batch(scope)
        registerAshP207Batch(scope)
        registerAshP208Batch(scope)
        registerAshP209Batch(scope)
        registerAshP210Batch(scope)
        registerAshP211Batch(scope)
        registerAshP212Batch(scope)
        registerAshP213Batch(scope)
        registerAshP214Batch(scope)
        registerAshP215Batch(scope)
        registerAshP216Batch(scope)
        registerAshP217Batch(scope)
        registerAshP218Batch(scope)
        registerAshP219Batch(scope)
        registerAshP220Batch(scope)
        registerAshP221Batch(scope)
        registerAshP222Batch(scope)
        registerAshP223Batch(scope)
        registerAshP224Batch(scope)
        registerAshP225Batch(scope)
        registerAshP226Batch(scope)
        registerAshP227Batch(scope)
        registerAshP228Batch(scope)
        registerAshP229Batch(scope)
        registerAshP230Batch(scope)
        registerAshP231Batch(scope)
        registerAshP232Batch(scope)
        registerAshP233Batch(scope)
        registerAshP234Batch(scope)
        registerAshP235Batch(scope)
        registerAshP236Batch(scope)
        registerAshP237Batch(scope)
        registerAshP238Batch(scope)
        registerPhase3110(scope)
        registerPhase3230(scope)
        registerPhase3350(scope)
        registerPhase3410(scope)
        registerPhase3470(scope)
        registerAshP239Batch(scope)
        registerAshP240Batch(scope)
        registerAshP241Batch(scope)
        registerAshP242Batch(scope)
        registerAshP243Batch(scope)
        registerAshP244Batch(scope)
        registerAshP245Batch(scope)
        registerAshP246Batch(scope)
        registerAshP247Batch(scope)
        registerAshP248Batch(scope)
        registerAshP249Batch(scope)
        registerAshP250Batch(scope)
        registerAshP251Batch(scope)
        registerAshP252Batch(scope)
        registerAshP253Batch(scope)
        registerAshP254Batch(scope)
        registerAshP255Batch(scope)
        registerAshP256Batch(scope)
        registerAshP257Batch(scope)
        registerAshP258Batch(scope)
        registerAshP259Batch(scope)
        registerAshP260Batch(scope)
        registerAshP261Batch(scope)
        registerAshP262Batch(scope)
        registerAshP263Batch(scope)
        registerAshP264Batch(scope)
        registerAshP265Batch(scope)
        registerAshP266Batch(scope)
        registerAshP267Batch(scope)
        registerAshP268Batch(scope)
        registerAshP269Batch(scope)
        registerAshP301Batch(scope)
        registerAshP305Batch(scope)
        registerAshP429Batch(scope)
        registerAshP430Batch(scope)
        registerAshP432Batch(scope)
        registerAshP481Batch(scope)
        registerAshP762Batch(scope)
        registerAshP763Batch(scope)
        registerAshP865Batch(scope)
        registerAshP889Batch(scope)
        registerAshP890Batch(scope)
        registerAshP891Batch(scope)
        registerAshP892Batch(scope)
        registerAshP893Batch(scope)
        registerAshP894Batch(scope)
        registerAshP895Batch(scope)
        registerAshP896Batch(scope)
        registerAshP897Batch(scope)
        registerAshP898Batch(scope)
        registerAshP899Batch(scope)
        registerAshP900Batch(scope)
        registerAshP901Batch(scope)
        registerAshP902Batch(scope)
        registerAshP903Batch(scope)
        registerAshP904Batch(scope)
        registerAshP905Batch(scope)
        registerAshP906Batch(scope)
        registerAshP907Batch(scope)
        registerAshP908Batch(scope)
        registerAshP909Batch(scope)
        registerAshP910Batch(scope)
        registerAshP911Batch(scope)
        registerAshP912Batch(scope)
        registerAshP913Batch(scope)
        registerAshP914Batch(scope)
        registerAshP915Batch(scope)
        registerAshP916Batch(scope)
        registerAshP917Batch(scope)
        registerAshP918Batch(scope)

        // AshP919–949 Tracks E–H
        registerAshP919TrackEBatch(scope)
        registerAshP928TrackFBatch(scope)
        registerAshP935TrackGBatch(scope)
        registerAshP943TrackHBatch(scope)

        // AshP950–1010 Tracks I–T (deepen + residual)
        registerAshP950TrackIBatch(scope)
        registerAshP956TrackJBatch(scope)
        registerAshP960TrackKBatch(scope)
        registerAshP968TrackLBatch(scope)
        registerAshP970TrackMBatch(scope)
        registerAshP973TrackNBatch(scope)
        registerAshP975TrackOBatch(scope)
        registerAshP981TrackPBatch(scope)
        registerAshP985TrackQBatch(scope)
        registerAshP991TrackRBatch(scope)
        registerAshP997TrackSBatch(scope)
        registerAshP1004TrackTBatch(scope)
        registerPhase3710(scope)
        registerPhase3770(scope)

        regFn(scope, "tower_door", AshType.BOOLEAN, emptyList()) { rt, _ ->
            runTowerDoor { message -> rt.print(message) }
            AshValue.TRUE
        }
        registerItemActions(scope)
        registerPricingQueries(scope)
        registerMallFunctions(scope)
        registerShopFunctions(scope)
        registerOutfitFunctions(scope)
        registerCoinmasterFunctions(scope)
        registerCraftFunctions(scope)
        registerBanishQueries(scope)
        registerWebRequests(scope)
        registerWebHtml(scope)
        registerHermit(scope)
        registerTimingAndLogging(scope)
        registerCliOutput(scope)
        registerEnvironmentQueries(scope)
        registerUneffectActions(scope)
        registerQuestQueries(scope)
        registerChatQueries(scope)
        registerScriptFunctions(scope)
        registerSessionLog(scope)
    }

    // ──────────────────────────────────────────────────────────────
    // Type conversion
    // ──────────────────────────────────────────────────────────────

    private fun registerTypeConversions(scope: AshScope) {
        // to_int
        register(scope, "to_int", AshType.INT, listOf("value" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().toLongOrNull() ?: 0L)
        }
        register(scope, "to_int", AshType.INT, listOf("value" to AshType.FLOAT)) { _, args ->
            AshValue.of(args[0].toLong())
        }
        register(scope, "to_int", AshType.INT, listOf("value" to AshType.BOOLEAN)) { _, args ->
            AshValue.of(if (args[0].toBoolean()) 1L else 0L)
        }
        register(scope, "to_int", AshType.INT, listOf("value" to AshType.INT)) { _, args ->
            args[0]
        }

        // to_float
        register(scope, "to_float", AshType.FLOAT, listOf("value" to AshType.INT)) { _, args ->
            AshValue.of(args[0].toDouble())
        }
        register(scope, "to_float", AshType.FLOAT, listOf("value" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().toDoubleOrNull() ?: 0.0)
        }
        register(scope, "to_float", AshType.FLOAT, listOf("value" to AshType.FLOAT)) { _, args ->
            args[0]
        }

        // to_boolean
        register(scope, "to_boolean", AshType.BOOLEAN, listOf("value" to AshType.INT)) { _, args ->
            AshValue.of(args[0].toLong() != 0L)
        }
        register(scope, "to_boolean", AshType.BOOLEAN, listOf("value" to AshType.STRING)) { _, args ->
            val s = args[0].toString()
            AshValue.of(s.isNotEmpty() && s != "false")
        }
        register(scope, "to_boolean", AshType.BOOLEAN, listOf("value" to AshType.BOOLEAN)) { _, args ->
            args[0]
        }

        // to_int for game entity types — returns the entity's numeric database ID
        // Returns 0 when gameDatabase is null (test/no-db context) or entity unknown.
        register(scope, "to_int", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
            AshValue.of(gameDatabase?.item(args[0].toString())?.id?.toLong() ?: 0L)
        }
        register(scope, "to_int", AshType.INT, listOf("ef" to AshType.EFFECT)) { _, args ->
            AshValue.of(gameDatabase?.effect(args[0].toString())?.id?.toLong() ?: 0L)
        }
        register(scope, "to_int", AshType.INT, listOf("sk" to AshType.SKILL)) { _, args ->
            AshValue.of(gameDatabase?.skill(args[0].toString())?.id?.toLong() ?: 0L)
        }
        register(scope, "to_int", AshType.INT, listOf("fa" to AshType.FAMILIAR)) { _, args ->
            AshValue.of(gameDatabase?.familiar(args[0].toString())?.id?.toLong() ?: 0L)
        }
        register(scope, "to_int", AshType.INT, listOf("loc" to AshType.LOCATION)) { _, args ->
            AshValue.of(
                gameDatabase?.zone(args[0].toString())
                    ?.snarfblat?.toIntOrNull()?.toLong() ?: 0L
            )
        }
        register(scope, "to_int", AshType.INT, listOf("mo" to AshType.MONSTER)) { _, args ->
            AshValue.of(gameDatabase?.monster(args[0].toString())?.id?.toLong() ?: 0L)
        }

        // to_string for game entity types
        for (entityType in listOf(
            AshType.ITEM, AshType.SKILL, AshType.EFFECT,
            AshType.FAMILIAR, AshType.LOCATION, AshType.MONSTER,
            AshType.CLASS, AshType.STAT, AshType.SLOT,
            AshType.ELEMENT, AshType.COINMASTER, AshType.PHYLUM, AshType.PATH
        )) {
            val capturedType = entityType
            register(scope, "to_string", AshType.STRING, listOf("value" to capturedType)) { _, args ->
                AshValue.of(args[0].toString())
            }
        }

        // to_location(string) → location — type conversion for locations
        register(scope, "to_location", AshType.LOCATION, listOf("name" to AshType.STRING)) { _, args ->
            val input = args[0].toString()
            val resolved = resolveLocation(input)?.name
                ?: net.sourceforge.kolmafia.modifiers.LocationNames.resolve(input)
            AshValue(AshType.LOCATION, resolved ?: "")
        }

        register(scope, "to_coinmaster", AshType.COINMASTER, listOf("name" to AshType.STRING)) { _, args ->
            val resolved = net.sourceforge.kolmafia.shop.CoinmasterRegistry.resolve(args[0].toString())
            AshValue(AshType.COINMASTER, resolved ?: "")
        }

        register(scope, "to_path", AshType.PATH, listOf("name" to AshType.STRING)) { _, args ->
            val resolved = net.sourceforge.kolmafia.modifiers.PathNames.resolve(args[0].toString())
            AshValue(AshType.PATH, resolved ?: "")
        }

        register(scope, "to_stat", AshType.STAT, listOf("name" to AshType.STRING)) { _, args ->
            AshValue(AshType.STAT, args[0].toString())
        }

        register(scope, "to_thrall", AshType.THRALL, listOf("name" to AshType.STRING)) { _, args ->
            val resolved = net.sourceforge.kolmafia.modifiers.ThrallNames.resolve(args[0].toString())
            AshValue(AshType.THRALL, resolved ?: "")
        }

        register(scope, "to_servant", AshType.SERVANT, listOf("name" to AshType.STRING)) { _, args ->
            val resolved = net.sourceforge.kolmafia.modifiers.ServantData.resolve(args[0].toString())
            AshValue(AshType.SERVANT, resolved?.type ?: "")
        }

        register(scope, "to_vykea", AshType.VYKEA, listOf("name" to AshType.STRING)) { _, args ->
            val resolved = net.sourceforge.kolmafia.modifiers.VykeaCompanionData.resolve(args[0].toString())
            AshValue(AshType.VYKEA, resolved ?: "")
        }

        register(scope, "to_bounty", AshType.BOUNTY, listOf("name" to AshType.STRING)) { _, args ->
            val resolved = net.sourceforge.kolmafia.data.BountyDatabase.resolve(args[0].toString())
            AshValue(AshType.BOUNTY, resolved ?: "")
        }

        register(scope, "to_modifier", AshType.MODIFIER, listOf("name" to AshType.STRING)) { _, args ->
            val resolved = net.sourceforge.kolmafia.modifiers.ModifierNames.byCaselessName(args[0].toString())
            AshValue(AshType.MODIFIER, resolved ?: "")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // String utilities
    // ──────────────────────────────────────────────────────────────

    private fun registerStringUtils(scope: AshScope) {
        register(scope, "length", AshType.INT, listOf("s" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().length)
        }
        register(scope, "substring", AshType.STRING,
            listOf("s" to AshType.STRING, "start" to AshType.INT, "end" to AshType.INT)) { _, args ->
            val s = args[0].toString()
            val start = args[1].toLong().toInt().coerceIn(0, s.length)
            // ASH end is inclusive: substring("hello",1,3) == "ell"
            val endInclusive = args[2].toLong().toInt().coerceIn(start - 1, s.length - 1)
            AshValue.of(s.substring(start, endInclusive + 1))
        }
        register(scope, "substring", AshType.STRING,
            listOf("s" to AshType.STRING, "start" to AshType.INT)) { _, args ->
            val s = args[0].toString()
            val start = args[1].toLong().toInt().coerceIn(0, s.length)
            AshValue.of(s.substring(start))
        }
        register(scope, "index_of", AshType.INT,
            listOf("source" to AshType.STRING, "search" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().indexOf(args[1].toString()).toLong())
        }
        register(scope, "index_of", AshType.INT,
            listOf("source" to AshType.STRING, "search" to AshType.STRING, "start" to AshType.INT)) { _, args ->
            val start = args[2].toLong().toInt()
            AshValue.of(args[0].toString().indexOf(args[1].toString(), start).toLong())
        }
        register(scope, "to_upper_case", AshType.STRING, listOf("s" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().uppercase())
        }
        register(scope, "to_lower_case", AshType.STRING, listOf("s" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().lowercase())
        }
        register(scope, "starts_with", AshType.BOOLEAN,
            listOf("s" to AshType.STRING, "prefix" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().startsWith(args[1].toString()))
        }
        register(scope, "ends_with", AshType.BOOLEAN,
            listOf("s" to AshType.STRING, "suffix" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().endsWith(args[1].toString()))
        }
        register(scope, "contains", AshType.BOOLEAN,
            listOf("s" to AshType.STRING, "sub" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().contains(args[1].toString()))
        }
        register(scope, "replace_string", AshType.STRING,
            listOf("s" to AshType.STRING, "old" to AshType.STRING, "new" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().replace(args[1].toString(), args[2].toString()))
        }
        register(scope, "trim", AshType.STRING, listOf("s" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().trim())
        }
        // split_string returns string[int] → AggregateType(indexType=INT, dataType=STRING)
        register(scope, "split_string", AggregateType(AshType.INT, AshType.STRING),
            listOf("s" to AshType.STRING, "sep" to AshType.STRING)) { _, args ->
            val parts = args[0].toString().split(args[1].toString())
            val result = AggregateValue(AggregateType(AshType.INT, AshType.STRING))
            parts.forEachIndexed { i, part -> result[AshValue.of(i)] = AshValue.of(part) }
            result
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Math utilities
    // ──────────────────────────────────────────────────────────────

    private fun registerMathUtils(scope: AshScope) {
        register(scope, "floor", AshType.INT, listOf("f" to AshType.FLOAT)) { _, args ->
            AshValue.of(floor(args[0].toDouble()).toLong())
        }
        register(scope, "ceil", AshType.INT, listOf("f" to AshType.FLOAT)) { _, args ->
            AshValue.of(ceil(args[0].toDouble()).toLong())
        }
        register(scope, "round", AshType.INT, listOf("f" to AshType.FLOAT)) { _, args ->
            AshValue.of(args[0].toDouble().roundToLong())
        }
        register(scope, "sqrt", AshType.FLOAT, listOf("f" to AshType.FLOAT)) { _, args ->
            AshValue.of(sqrt(args[0].toDouble()))
        }
        register(scope, "abs", AshType.INT, listOf("n" to AshType.INT)) { _, args ->
            AshValue.of(abs(args[0].toLong()))
        }
        register(scope, "abs", AshType.FLOAT, listOf("f" to AshType.FLOAT)) { _, args ->
            AshValue.of(abs(args[0].toDouble()))
        }
        register(scope, "max", AshType.INT, listOf("a" to AshType.INT, "b" to AshType.INT)) { _, args ->
            AshValue.of(maxOf(args[0].toLong(), args[1].toLong()))
        }
        register(scope, "max", AshType.FLOAT, listOf("a" to AshType.FLOAT, "b" to AshType.FLOAT)) { _, args ->
            AshValue.of(maxOf(args[0].toDouble(), args[1].toDouble()))
        }
        register(scope, "min", AshType.INT, listOf("a" to AshType.INT, "b" to AshType.INT)) { _, args ->
            AshValue.of(minOf(args[0].toLong(), args[1].toLong()))
        }
        register(scope, "min", AshType.FLOAT, listOf("a" to AshType.FLOAT, "b" to AshType.FLOAT)) { _, args ->
            AshValue.of(minOf(args[0].toDouble(), args[1].toDouble()))
        }
        register(scope, "random", AshType.FLOAT, listOf("limit" to AshType.FLOAT)) { _, args ->
            AshValue.of(Random.nextDouble() * args[0].toDouble())
        }
        register(scope, "pow", AshType.FLOAT, listOf("base" to AshType.FLOAT, "exp" to AshType.FLOAT)) { _, args ->
            AshValue.of(args[0].toDouble().pow(args[1].toDouble()))
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Aggregate utilities
    //
    // Fix: canCoerce already returns true for aggregate→aggregate (added in AshType),
    // so registering one concrete aggregate type as the parameter type is enough —
    // the resolver will accept any aggregate argument.
    // ──────────────────────────────────────────────────────────────

    private fun registerAggregateUtils(scope: AshScope) {
        // AshType.AGGREGATE is a sentinel: canCoerce(anyConcreteAggregate, AGGREGATE) == true,
        // so count/clear accept any aggregate type without needing per-type overloads.
        register(scope, "count", AshType.INT, listOf("agg" to AshType.AGGREGATE)) { _, args ->
            AshValue.of((args[0] as? AggregateValue)?.map?.size?.toLong() ?: 0L)
        }
        register(scope, "clear", AshType.VOID, listOf("agg" to AshType.AGGREGATE)) { _, args ->
            (args[0] as? AggregateValue)?.map?.clear()
            AshValue.VOID
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Print utilities
    // ──────────────────────────────────────────────────────────────

    private fun registerPrintUtils(scope: AshScope) {
        // print(string) already registered in super
        register(scope, "print_html", AshType.VOID, listOf("html" to AshType.STRING)) { runtime, args ->
            val stripped = args[0].toString()
                .replace(Regex("<[^>]+>"), "")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
            runtime.print(stripped)
            AshValue.VOID
        }
        register(scope, "print_to_string", AshType.STRING, listOf("value" to AshType.STRING)) { _, args ->
            args[0]
        }
        register(scope, "abort", AshType.VOID, listOf("msg" to AshType.STRING)) { _, args ->
            throw ScriptException(args[0].toString())
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Character state queries
    // ──────────────────────────────────────────────────────────────

    private fun registerCharacterQueries(scope: AshScope) {
        register(scope, "my_name", AshType.STRING, emptyList()) { _, _ ->
            AshValue.of(character?.state?.value?.name ?: "")
        }
        register(scope, "my_level", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.level ?: 1).toLong())
        }
        register(scope, "my_hp", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.currentHp ?: 0).toLong())
        }
        register(scope, "my_maxhp", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.maxHp ?: 0).toLong())
        }
        register(scope, "my_mp", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.currentMp ?: 0).toLong())
        }
        register(scope, "my_maxmp", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.maxMp ?: 0).toLong())
        }
        register(scope, "my_meat", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.meat ?: 0).toLong())
        }
        register(scope, "my_adventures", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.adventuresLeft ?: 0).toLong())
        }
        register(scope, "my_fullness", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.fullness ?: 0).toLong())
        }
        register(scope, "my_inebriety", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.inebriety ?: 0).toLong())
        }
        register(scope, "my_spleen_use", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.spleenUsed ?: 0).toLong())
        }
        register(scope, "is_headless", AshType.BOOLEAN, emptyList()) { _, _ ->
            AshValue.of(true)
        }
        register(scope, "my_basestat", AshType.INT, listOf("stat" to AshType.STAT)) { _, args ->
            val cs = character?.state?.value
            AshValue.of(if (cs == null) 0L else StatNames.baseValue(cs, args[0].toString()))
        }
        register(scope, "in_hardcore", AshType.BOOLEAN, emptyList()) { _, _ ->
            AshValue.of(character?.state?.value?.isHardcore ?: false)
        }
        register(scope, "my_familiar", AshType.FAMILIAR, emptyList()) { _, _ ->
            AshValue.familiar(
                familiarManager?.state?.value?.activeFamiliar?.race
                    ?.takeIf { it.isNotBlank() } ?: "none"
            )
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Item queries
    // ──────────────────────────────────────────────────────────────

    private fun registerItemQueries(scope: AshScope) {
        fun inventoryQty(name: String): Long {
            val qty = inventoryManager?.state?.value?.items?.values
                ?.find { it.name.equals(name, ignoreCase = true) }?.quantity ?: 0
            return qty.toLong()
        }

        register(scope, "item_amount", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
            AshValue.of(inventoryQty(args[0].toString()))
        }
        register(scope, "item_count", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
            AshValue.of(inventoryQty(args[0].toString()))
        }
        register(scope, "available_amount", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
            val name = args[0].toString()
            val itemId = gameDatabase?.item(name)?.id
                ?: inventoryManager?.state?.value?.items?.values
                    ?.find { it.name.equals(name, ignoreCase = true) }?.itemId
            if (itemId == null) return@register AshValue.of(0L)
            val count = kotlinx.coroutines.runBlocking {
                physicalAccessibleCount(itemId, name)
            }
            AshValue.of(count.toLong())
        }
        register(scope, "to_item", AshType.ITEM, listOf("name" to AshType.STRING)) { _, args ->
            AshValue.item(args[0].toString())
        }
        register(scope, "to_item", AshType.ITEM, listOf("id" to AshType.INT)) { _, args ->
            val id = args[0].toLong().toInt()
            val name = inventoryManager?.state?.value?.items?.values
                ?.find { it.itemId == id }?.name ?: id.toString()
            AshValue.item(name)
        }
        register(scope, "have_item", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
            val name = args[0].toString()
            val qty = inventoryManager?.state?.value?.items?.values
                ?.find { it.name.equals(name, ignoreCase = true) }?.quantity ?: 0
            AshValue.of(qty > 0)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Skill queries
    // ──────────────────────────────────────────────────────────────

    private fun registerSkillQueries(scope: AshScope) {
        register(scope, "have_skill", AshType.BOOLEAN, listOf("sk" to AshType.SKILL)) { _, args ->
            val name = args[0].toString()
            val has = resolvedSkillNames().any { it.equals(name, ignoreCase = true) }
            AshValue.of(has)
        }
        register(scope, "mp_cost", AshType.INT, listOf("sk" to AshType.SKILL)) { _, args ->
            val name = args[0].toString()
            val cost = skillManager?.state?.value?.skills
                ?.find { it.name.equals(name, ignoreCase = true) }?.mpCost ?: 0
            AshValue.of(cost.toLong())
        }
        register(scope, "to_skill", AshType.SKILL, listOf("name" to AshType.STRING)) { _, args ->
            AshValue.skill(args[0].toString())
        }
        register(scope, "daily_limit", AshType.INT, listOf("sk" to AshType.SKILL)) { _, args ->
            val name = args[0].toString()
            val limit = skillManager?.state?.value?.skills
                ?.find { it.name.equals(name, ignoreCase = true) }?.dailyLimit ?: 0
            AshValue.of(limit.toLong())
        }
        register(scope, "times_cast", AshType.INT, listOf("sk" to AshType.SKILL)) { _, args ->
            val name = args[0].toString()
            val cast = skillManager?.state?.value?.skills
                ?.find { it.name.equals(name, ignoreCase = true) }?.timesCast ?: 0
            AshValue.of(cast.toLong())
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Effect queries
    // ──────────────────────────────────────────────────────────────

    private fun registerEffectQueries(scope: AshScope) {
        register(scope, "have_effect", AshType.INT, listOf("ef" to AshType.EFFECT)) { _, args ->
            val name = args[0].toString()
            val duration = effectManager?.state?.value?.effects
                ?.find { it.name.equals(name, ignoreCase = true) }?.duration ?: 0
            AshValue.of(duration.toLong())
        }
        register(scope, "to_effect", AshType.EFFECT, listOf("name" to AshType.STRING)) { _, args ->
            AshValue.effect(args[0].toString())
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Game actions (suspend-aware wrappers)
    // ──────────────────────────────────────────────────────────────

    private fun registerGameActions(scope: AshScope) {
        register(scope, "adventure", AshType.BOOLEAN,
            listOf("turns" to AshType.INT, "loc" to AshType.LOCATION)) { _, args ->
            val turns = args[0].toLong().toInt()
            val locName = args[1].toString()
            val manager = adventureManager
                ?: throw ScriptException("Adventure manager not available")
            val location = resolveLocation(locName)
                ?: throw ScriptException("Unknown location: $locName")
            kotlinx.coroutines.runBlocking {
                manager.runAdventures(location, turns, this).join()
            }
            AshValue.of(true)
        }

        // adv1(loc: location, adventuresUsed: int) → boolean
        // Runs a single adventure at loc. Returns false if no AdventureManager.
        register(scope, "adv1", AshType.BOOLEAN,
            listOf("loc" to AshType.LOCATION, "adventuresUsed" to AshType.INT)) { _, args ->
            val locName = args[0].toString()
            val manager = adventureManager ?: return@register AshValue.of(false)
            val location = resolveLocation(locName) ?: return@register AshValue.of(false)
            kotlinx.coroutines.runBlocking {
                manager.runAdventures(location, 1, this).join()
            }
            AshValue.of(true)
        }

        register(scope, "use_skill", AshType.BOOLEAN,
            listOf("turns" to AshType.INT, "sk" to AshType.SKILL)) { _, args ->
            val count = args[0].toLong().toInt()
            val skillName = args[1].toString()
            val manager = skillManager
                ?: throw ScriptException("Skill manager not available")
            val skill = manager.state.value.skills
                .find { it.name.equals(skillName, ignoreCase = true) }
                ?: throw ScriptException("Unknown skill: $skillName")
            kotlinx.coroutines.runBlocking {
                repeat(count) { manager.cast(skill, 1) }
            }
            AshValue.of(true)
        }

        register(scope, "use_skill", AshType.BOOLEAN,
            listOf("sk" to AshType.SKILL)) { _, args ->
            val skillName = args[0].toString()
            val manager = skillManager
                ?: throw ScriptException("Skill manager not available")
            val skill = manager.state.value.skills
                .find { it.name.equals(skillName, ignoreCase = true) }
                ?: throw ScriptException("Unknown skill: $skillName")
            kotlinx.coroutines.runBlocking { manager.cast(skill, 1) }
            AshValue.of(true)
        }

        register(scope, "cli_execute", AshType.BOOLEAN, listOf("cmd" to AshType.STRING)) { runtime, args ->
            lastCliOutput.clear()
            val capturing = CliCapturingContext(runtime, lastCliOutput)
            try {
                dispatchCli(args[0].toString(), capturing)
            } catch (e: ScriptException) {
                throw e
            }
            AshValue.of(true)
        }

    }

    /** Wraps an [AshRuntimeContext] to mirror [print] into [lastCliOutput]. */
    private class CliCapturingContext(
        private val delegate: AshRuntimeContext,
        private val buffer: StringBuilder,
    ) : AshRuntimeContext {
        override fun print(msg: String) {
            buffer.append(msg).append('\n')
            delegate.print(msg)
        }

        override fun lastCombatAction(): String =
            if (delegate is AshRuntime) delegate.lastCombatAction() else delegate.lastCombatAction()

        override fun setCombatAction(action: String) {
            if (delegate is AshRuntime) delegate.setCombatAction(action)
            else delegate.setCombatAction(action)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Banish queries
    // ──────────────────────────────────────────────────────────────

    private fun registerBanishQueries(scope: AshScope) {
        // is_banished(monster) → boolean — accepts both monster type and string
        register(scope, "is_banished", AshType.BOOLEAN, listOf("monster" to AshType.MONSTER)) { _, args ->
            val name = args[0].toString()
            val currentTurn = character?.state?.value?.currentRun ?: 0
            AshValue.of(banishManager?.isBanished(name, currentTurn) ?: false)
        }
        register(scope, "is_banished", AshType.BOOLEAN, listOf("monster" to AshType.STRING)) { _, args ->
            val name = args[0].toString()
            val currentTurn = character?.state?.value?.currentRun ?: 0
            AshValue.of(banishManager?.isBanished(name, currentTurn) ?: false)
        }

        // banishers_used() → string[monster]
        val returnType = AggregateType(AshType.MONSTER, AshType.STRING)
        register(scope, "banishers_used", returnType, emptyList()) { _, _ ->
            val result = AggregateValue(returnType)
            val currentTurn = character?.state?.value?.currentRun ?: 0
            banishManager?.getActiveBanishes(currentTurn)
                ?.forEach { (monsterName, banisher) ->
                    result[AshValue(AshType.MONSTER, monsterName)] = AshValue.of(banisher.canonicalName)
                }
            result
        }
    }
}
