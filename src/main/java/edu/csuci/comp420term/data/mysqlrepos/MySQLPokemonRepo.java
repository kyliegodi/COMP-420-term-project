package edu.csuci.comp420term.data.mysqlrepos;

import edu.csuci.comp420term.application.ApplicationContext;
import edu.csuci.comp420term.data.ConnectionBuilder;
import edu.csuci.comp420term.entities.*;
import edu.csuci.comp420term.repos.PokemonRepository;
import edu.csuci.comp420term.utils.EntityCache;
import edu.csuci.comp420term.utils.OrderedPair;

import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MySQLPokemonRepo implements PokemonRepository {

    private static final String SELECT_POKEMON_BY_ID = "SELECT * FROM POKEMON WHERE POKEMON_ID = ? LIMIT 1;";
    private static final String CALL_ALTERNATIVE_FORMS = "{CALL alternative_forms(?)}";
    private static final String SELECT_EVOLUTION_METHODS = "SELECT *\n" +
            "FROM EVOLUTION_METHOD\n" +
            "WHERE POKEMON_ID = ?\n" +
            "ORDER BY EVOLVES_INTO_ID;";
    private static final String SELECT_ALL_POKEMON = "SELECT * FROM POKEMON ORDER BY POKEMON_ID";
    public static final String SELECT_FULL_POKEMON_RANGE = "SELECT MIN(POKEMON_ID), MAX(POKEMON_ID) FROM POKEMON;";
    private final EntityCache<Integer, Pokemon> pokemonCache;
    private final EntityCache<Integer, OrderedPair<Integer>> generationRangeCache;

    public MySQLPokemonRepo() {
        pokemonCache = new EntityCache<>();
        generationRangeCache = new EntityCache<>();
    }

    @Override
    public Pokemon findById(final int id) throws SQLException {
        Optional<Pokemon> cachedPokemon = pokemonCache.get(id);
        if (cachedPokemon.isPresent()) {
            return cachedPokemon.get();
        }
        try (Connection connection = ConnectionBuilder.buildConnection();
             PreparedStatement selectPokemonStatement = connection.prepareStatement(SELECT_POKEMON_BY_ID)) {
            selectPokemonStatement.setInt(1, id);
            try (ResultSet pokemonResult = selectPokemonStatement.executeQuery()) {
                verifyPokemonOfIdExistsInResultSet(id, pokemonResult);
                Pokemon pokemon = buildPokemonFromResultSet(connection, pokemonResult);
                cachePokemon(pokemon);
                return pokemon;
            }

        }
    }

    private Pokemon buildPokemonFromResultSet(Connection connection, ResultSet resultSet) throws SQLException {
        final int pokemonId = resultSet.getInt("POKEMON_ID");
        final String pokemonName = resultSet.getString("POKEMON_NAME");
        final String pokemonDescription = resultSet.getString("POKEMON_DESCRIPTION");
        final String pokemonImageURL = resultSet.getString("POKEMON_IMAGE_URL");
        final Optional<Type> primaryType = getTypeFromResultSet(resultSet, "PRIMARY_TYPE");
        final Optional<Type> secondaryType = getTypeFromResultSet(resultSet, "SECONDARY_TYPE");
        final Optional<EggGroup> primaryEggGroup = getEggGroupFromResultSet(resultSet, "PRIMARY_EGG_GROUP");
        final Optional<EggGroup> secondaryEggGroup = getEggGroupFromResultSet(resultSet, "SECONDARY_EGG_GROUP");
        final Optional<Ability> primaryAbility = getAbilityFromResultSet(resultSet, "PRIMARY_ABILITY");
        final Optional<Ability> secondaryAbility = getAbilityFromResultSet(resultSet, "SECONDARY_ABILITY");
        final Optional<Ability> hiddenAbility = getAbilityFromResultSet(resultSet, "HIDDEN_ABILITY");
        final List<BaseStat> baseStats = ApplicationContext.appContext().statRepository.pokemonBaseStats(pokemonId);
        final List<AlternateForm> alternateForms = getAlternateForms(connection, pokemonId);
        final List<EvolutionMethod> evolutionMethods = getEvolutionMethods(connection, pokemonId);
        return new Pokemon(
                pokemonId,
                primaryType.orElseThrow(),
                secondaryType.orElse(null),
                primaryEggGroup.orElseThrow(),
                secondaryEggGroup.orElse(null),
                primaryAbility.orElseThrow(),
                secondaryAbility.orElse(null),
                hiddenAbility.orElse(null),
                pokemonName,
                pokemonDescription,
                pokemonImageURL,
                baseStats,
                alternateForms,
                evolutionMethods
        );
    }

    private void cachePokemon(Pokemon pokemon) {
        pokemonCache.cache(pokemon.id, pokemon);
    }

    private List<EvolutionMethod> getEvolutionMethods(Connection connection, int pokemonId) throws SQLException {
        final List<EvolutionMethod> evolutionMethods = new ArrayList<>();
        try (PreparedStatement selectStatement = connection.prepareStatement(SELECT_EVOLUTION_METHODS)) {
            selectStatement.setInt(1, pokemonId);
            try (ResultSet resultSet = selectStatement.executeQuery()) {
                while (resultSet.next()) {
                    evolutionMethods.add(buildEvolutionMethodFromResultSet(resultSet));
                }
            }
        }
        return evolutionMethods;
    }

    private EvolutionMethod buildEvolutionMethodFromResultSet(ResultSet resultSet) throws SQLException {
        final int pokemonId1 = resultSet.getInt("POKEMON_ID");
        final int evolvesIntoId = resultSet.getInt("EVOLVES_INTO_ID");
        final String method = resultSet.getString("METHOD_DESCRIPTION");
        return new EvolutionMethod(pokemonId1, evolvesIntoId, method);
    }

    private List<AlternateForm> getAlternateForms(Connection connection, int pokemonId) throws SQLException {
        List<AlternateForm> alternateForms = new ArrayList<>();
        try (CallableStatement alternateFormStatement = connection.prepareCall(CALL_ALTERNATIVE_FORMS)) {
            alternateFormStatement.setInt(1, pokemonId);
            try (ResultSet resultSet = alternateFormStatement.executeQuery()) {
                while (resultSet.next()) {
                    alternateForms.add(buildAlternateFormFromResultSet(resultSet));
                }
            }
        }
        return alternateForms;
    }

    private AlternateForm buildAlternateFormFromResultSet(ResultSet resultSet) throws SQLException {
        final int id = resultSet.getInt("ALTERNATE_FORM_ID");
        final int pokemonId = resultSet.getInt("POKEMON_ID");
        final String formName = resultSet.getString("ALTERNATE_FORM_NAME");
        final String formImageUrl = resultSet.getString("ALTERNATE_FORM_IMAGE_URL");
        return new AlternateForm(id, pokemonId, formName, formImageUrl);
    }

    private Optional<Ability> getAbilityFromResultSet(ResultSet pokemonResult, String abilityColumnName) throws SQLException {
        Optional<Ability> abilityOptional = Optional.empty();
        final int abilityId = pokemonResult.getInt(abilityColumnName);
        if (!pokemonResult.wasNull()) {
            final Ability ability = ApplicationContext.appContext().abilityRepo.findById(abilityId);
            abilityOptional = Optional.of(ability);
        }
        return abilityOptional;
    }

    private Optional<EggGroup> getEggGroupFromResultSet(ResultSet pokemonResult, String eggGroupColumnName) throws SQLException {
        Optional<EggGroup> eggGroupOptional = Optional.empty();
        final int eggGroupId = pokemonResult.getInt(eggGroupColumnName);
        if (!pokemonResult.wasNull()) {
            final EggGroup eggGroup = ApplicationContext.appContext().eggGroupRepo.findById(eggGroupId);
            eggGroupOptional = Optional.of(eggGroup);
        }
        return eggGroupOptional;
    }

    private Optional<Type> getTypeFromResultSet(ResultSet pokemonResult, String typeIdColumnName) throws SQLException {
        Optional<Type> typeOptional = Optional.empty();
        final int typeId = pokemonResult.getInt(typeIdColumnName);
        if (!pokemonResult.wasNull()) {
            final Type type = ApplicationContext.appContext().typeRepo.findById(typeId);
            typeOptional = Optional.of(type);
        }
        return typeOptional;
    }

    private void verifyPokemonOfIdExistsInResultSet(int id, ResultSet pokemonResult) throws SQLException {
        if (!pokemonResult.next()) throw new SQLException(String.format("Pokemon with id %d could not be found", id));
    }

    private OrderedPair<Integer> pokemonRange() throws SQLException {
        try (Connection connection = ConnectionBuilder.buildConnection();
             PreparedStatement selectStatement = connection.prepareStatement(SELECT_FULL_POKEMON_RANGE);
             ResultSet resultSet = selectStatement.executeQuery()) {
            if (resultSet.next()) {
                return buildRangeFromResultSet(resultSet);
            }
        }
        return new OrderedPair<>(-1, -1);
    }

    private OrderedPair<Integer> buildRangeFromResultSet(ResultSet resultSet) throws SQLException {
        final int min = resultSet.getInt(1);
        final int max = resultSet.getInt(2);
        return new OrderedPair<>(min, max);
    }

    @Override
    public List<Pokemon> findAll() throws SQLException {
        ApplicationContext.appContext().abilityRepo.findAll();
        ApplicationContext.appContext().eggGroupRepo.findAll();
        ApplicationContext.appContext().typeRepo.findAll();
        return findWithinRange(pokemonRange());
    }

    @Override
    public List<Pokemon> findAllInPage(int pageNumber, int pageSize, int offset, int max) throws SQLException {
        final int start = Integer.min(offset + pageNumber * pageSize, max);
        final int end = Integer.min(start + pageSize - 1, max);
        final OrderedPair<Integer> range = new OrderedPair<>(start, end);
        return findWithinRange(range);
    }

    private OrderedPair<Integer> generationRange(int generation) throws SQLException {
        final Optional<OrderedPair<Integer>> cachedRange = generationRangeCache.get(generation);
        if (cachedRange.isPresent()) {
            return cachedRange.get();
        }
        try (Connection connection = ConnectionBuilder.buildConnection();
             CallableStatement callGenerationSP = connection.prepareCall("{CALL search_by_generation(?)}")) {
            callGenerationSP.setInt(1, generation);
            try (ResultSet resultSet = callGenerationSP.executeQuery()) {
                if (resultSet.next()) {
                    final OrderedPair<Integer> generationRange = buildRangeFromResultSet(resultSet);
                    generationRangeCache.cache(generation, generationRange, Duration.ofHours(1));
                    return generationRange;
                }
            }
        }
        return new OrderedPair<>(-1, -1);
    }

    @Override
    public List<Pokemon> findAllInGeneration(int generation) throws SQLException {
        final OrderedPair<Integer> generationRange = generationRange(generation);
        return findWithinRange(generationRange);
    }

    @Override
    public List<Pokemon> findAllInGenerationPage(int generation, int pageNumber, int pageSize) throws SQLException {
        final OrderedPair<Integer> generationRange = generationRange(generation);
        return findAllInPage(pageNumber, pageSize, generationRange.first, generationRange.second);
    }

    @Override
    public int pagesInGeneration(int generation, int pageSize) throws SQLException {
        return this.pagesInRange(generationRange(generation), pageSize);
    }

    private List<Pokemon> findWithinRange(OrderedPair<Integer> range) throws SQLException {
        if (range.first > range.second) return findWithinRange(OrderedPair.ensureRange(range));
        ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final List<Pokemon> pokemonList = new ArrayList<>();
        for (int i = range.first; i <= range.second; i++) {
            final int id = i;
            threadPool.execute(() -> {
                Optional<Pokemon> pokemonOptional;
                try {
                    pokemonOptional = Optional.of(findById(id));
                } catch (Exception e) {
                    pokemonOptional = Optional.empty();
                }
                pokemonOptional.ifPresent(pokemon -> {
                    synchronized (MySQLPokemonRepo.this) {
                        cachePokemon(pokemon);
                        pokemonList.add(pokemon);
                    }
                });
            });
        }
        try {
            threadPool.shutdown();
            if (!threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
        pokemonList.sort(Comparator.comparing(pokemon -> pokemon.id));
        return pokemonList;
    }
}
